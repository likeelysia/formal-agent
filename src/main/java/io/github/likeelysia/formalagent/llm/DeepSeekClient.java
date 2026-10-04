package io.github.likeelysia.formalagent.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.likeelysia.formalagent.chat.Message;
import io.github.likeelysia.formalagent.config.AppConfig;
import io.github.likeelysia.formalagent.exception.AgentException;
import io.github.likeelysia.formalagent.llm.dto.ChatRequest;
import io.github.likeelysia.formalagent.llm.dto.ChatResponse;
import io.github.likeelysia.formalagent.log.ApiLogger;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DeepSeekClient implements LlmClient {

    private final HttpClient client;                     // 重对象:构造时建一份,复用
    private final ObjectMapper mapper;                   // ← 容器注入的统一 JSON 序列化器
    private final AppConfig config;                      // ← 配置也由容器塞进来

    public DeepSeekClient(AppConfig config, ObjectMapper mapper) {   // 单构造器,@Autowired 可省
        this.config = config;
        this.mapper = mapper;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(config.timeoutSeconds()))
                .build();
    }

    @Override
    public String chat(List<Message> history, ChatOptions options) {
        String apiKey = System.getenv("DEEPSEEK_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new AgentException("没读到 DEEPSEEK_API_KEY —— 先配置环境变量再重启 IDEA");
        }
        try {
            // 强类型 DTO → JSON(取代手拼字符串模板)
            String body = mapper.writeValueAsString(new ChatRequest(
                    config.model(), history, config.maxTokens(),
                    options.jsonMode() ? ChatRequest.ResponseFormat.JSON : null));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.apiUrl()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            long start = System.currentTimeMillis();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            long cost = System.currentTimeMillis() - start;

            ApiLogger.record(config.model(), response.statusCode(), cost,
                    body.length(), response.body().length());
            if (config.debug()) System.out.println("[debug] 请求体:" + body);

            // 非 2xx 一律当失败:把状态码和返回体带上,方便定位(别解析了才发现空)
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new AgentException("DeepSeek 返回错误(" + response.statusCode() + "):"
                        + abbreviate(response.body()));
            }

            // JSON → 强类型 DTO(取代 readTree().path().path() 的裸导航)
            ChatResponse parsed = mapper.readValue(response.body(), ChatResponse.class);
            if (parsed.choices() == null || parsed.choices().isEmpty()) {
                throw new AgentException("DeepSeek 返回里没有 choices:" + abbreviate(response.body()));
            }
            return parsed.choices().get(0).message().content().strip();

        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();  // ① 恢复中断标记
            throw new AgentException("调用 DeepSeek 失败:" + e.getMessage(), e);        // ② 包装成自己的异常
        }
    }

    /** 出错时截断返回体,避免把整页 HTML/JSON 刷进日志。 */
    private static String abbreviate(String s) {
        if (s == null) return "";
        String one = s.replaceAll("\\s+", " ").strip();
        return one.length() <= 300 ? one : one.substring(0, 300) + "...";
    }
}
