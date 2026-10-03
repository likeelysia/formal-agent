package io.github.likeelysia.formalagent.llm;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.likeelysia.formalagent.chat.Message;
import io.github.likeelysia.formalagent.config.AppConfig;
import org.springframework.stereotype.Component;
import io.github.likeelysia.formalagent.exception.AgentException;
import io.github.likeelysia.formalagent.log.ApiLogger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class DeepSeekClient implements LlmClient {
    private final HttpClient client;                     // 重对象:构造时建一份,复用
    private final ObjectMapper mapper = new ObjectMapper();
    private final AppConfig config;                      // ← 新增:配置也由容器塞进来

    public DeepSeekClient(AppConfig config) {            // ← 单构造器,@Autowired 可省
        this.config = config;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(config.timeoutSeconds()))
                .build();
    }
    @Override
    public String chat(List<Message> history) {
        String apiKey = System.getenv("DEEPSEEK_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new AgentException("没读到 DEEPSEEK_API_KEY —— 先配置环境变量再重启 IDEA");
        }
        try {
            String body = buildRequestBody(history);                  // ← 抽出来,以后可单独测
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.apiUrl()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            long start = System.currentTimeMillis();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            long cost = System.currentTimeMillis() - start;

            JsonNode root = mapper.readTree(response.body());
            String answer = root.path("choices").path(0).path("message").path("content").asText().strip();

            ApiLogger.record(config.model(), response.statusCode(), cost, body.length(), answer.length());
            if (config.debug()) System.out.println("[debug] 请求体:" + body);
            return answer;

        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();  // ① 恢复中断标记
            throw new AgentException("调用 DeepSeek 失败:" + e.getMessage(), e);        // ② 包装成自己的异常
        }
    }

    /** 把消息历史拼成请求体 JSON(从原来的 main 里搬出来) */
    private String buildRequestBody(List<Message> history) throws JsonProcessingException {
        List<Map<String, String>> list = new ArrayList<>();
        for (Message m : history) list.add(Map.of("role", m.getRole(), "content", m.getContent()));
        return """
                 {"model": "%s", "messages": %s, "max_tokens": %d}
                 """.formatted(config.model(), mapper.writeValueAsString(list), config.maxTokens());
    }
}
