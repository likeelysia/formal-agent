package io.github.likeelysia.formalagent.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.likeelysia.formalagent.chat.Message;
import io.github.likeelysia.formalagent.config.AppConfig;
import io.github.likeelysia.formalagent.exception.AgentException;
import io.github.likeelysia.formalagent.llm.dto.AgentMessage;
import io.github.likeelysia.formalagent.llm.dto.AssistantTurn;
import io.github.likeelysia.formalagent.llm.dto.ChatRequest;
import io.github.likeelysia.formalagent.llm.dto.ChatResponse;
import io.github.likeelysia.formalagent.llm.dto.ToolDefinition;
import io.github.likeelysia.formalagent.log.ApiLogger;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
public class DeepSeekClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekClient.class);

    private final HttpClient client;                     // 重对象:构造时建一份,复用
    private final ObjectMapper mapper;                   // ← 容器注入的统一 JSON 序列化器
    private final AppConfig config;                      // ← 配置也由容器塞进来

    public DeepSeekClient(AppConfig config, ObjectMapper mapper) {   // 单构造器,@Autowired 可省
        this.config = config;
        this.mapper = mapper;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(config.llm().connectTimeoutSeconds()))
                .build();
    }

    @Retryable(
            value = TransientApiException.class,          // 只重试"瞬时故障"
            maxAttemptsExpression = "${fa.llm.max-attempts:5}",
            backoff = @Backoff(
                    delayExpression = "${fa.llm.retry-base-ms:2000}",
                    maxDelayExpression = "${fa.llm.retry-max-ms:20000}",
                    multiplier = 2.0,
                    random = false))
    @Override
    public String chat(List<Message> history, ChatOptions options) {
        ChatRequest request = new ChatRequest(
                config.deepseek().model(),
                history.stream().map(AgentMessage::of).toList(),
                config.deepseek().maxTokens(),
                options.jsonMode() ? ChatRequest.ResponseFormat.JSON : null,
                null);
        String content = post(request).choices().get(0).message().content();
        return content == null ? "" : content.strip();
    }

    @Retryable(
            value = TransientApiException.class,
            maxAttemptsExpression = "${fa.llm.max-attempts:5}",
            backoff = @Backoff(
                    delayExpression = "${fa.llm.retry-base-ms:2000}",
                    maxDelayExpression = "${fa.llm.retry-max-ms:20000}",
                    multiplier = 2.0,
                    random = false))
    @Override
    public AssistantTurn chatWithTools(List<AgentMessage> messages, List<ToolDefinition> tools) {
        ChatRequest request = new ChatRequest(
                config.deepseek().model(), messages,
                config.deepseek().maxTokens(), null, tools);
        ChatResponse.ChatMessage message = post(request).choices().get(0).message();
        return new AssistantTurn(message.content(), message.toolCalls());
    }

    /** 真正发请求的地方 —— 普通对话和工具对话共用这一份。 */
    private ChatResponse post(ChatRequest request) {
        String apiKey = System.getenv("DEEPSEEK_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new AgentException("没读到 DEEPSEEK_API_KEY —— 先配置环境变量再重启 IDEA");
        }
        try {
            String body = mapper.writeValueAsString(request);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(config.deepseek().apiUrl()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(config.llm().requestTimeoutSeconds()))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            long start = System.currentTimeMillis();
            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            long cost = System.currentTimeMillis() - start;

            ApiLogger.record(config.deepseek().model(), response.statusCode(), cost,
                    body.length(), response.body().length());
            if (config.debug()) log.debug("请求体:{}", body);

            int code = response.statusCode();
            if (code == 429 || code >= 500) {
                // 瞬时故障 → 抛可重试异常(交给 @Retryable 重试)
                throw new TransientApiException("DeepSeek 暂时不可用(" + code + "):" + abbreviate(response.body()));
            }
            if (code < 200 || code >= 300) {
                // 业务错误 → 直接失败,不重试
                throw new AgentException("DeepSeek 返回错误(" + code + "):" + abbreviate(response.body()));
            }

            ChatResponse parsed = mapper.readValue(response.body(), ChatResponse.class);
            if (parsed.choices() == null || parsed.choices().isEmpty()) {
                throw new TransientApiException("DeepSeek 返回里没有 choices:" + abbreviate(response.body()));
            }
            return parsed;

        } catch (JsonProcessingException e) {          // 注意:它是 IOException 的子类,要先接
            throw new AgentException("JSON 处理失败:" + e.getMessage(), e);
        } catch (IOException e) {
            throw new TransientApiException("网络异常:" + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AgentException("调用被中断", e);
        }
    }

    /** 出错时截断返回体,避免把整页 HTML/JSON 刷进日志。 */
    private static String abbreviate(String s) {
        if (s == null) return "";
        String one = s.replaceAll("\\s+", " ").strip();
        return one.length() <= 300 ? one : one.substring(0, 300) + "...";
    }
}
