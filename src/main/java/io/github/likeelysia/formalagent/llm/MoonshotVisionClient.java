package io.github.likeelysia.formalagent.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.likeelysia.formalagent.config.AppConfig;
import io.github.likeelysia.formalagent.exception.AgentException;
import io.github.likeelysia.formalagent.llm.dto.ChatResponse;
import io.github.likeelysia.formalagent.llm.dto.VisionRequest;
import io.github.likeelysia.formalagent.log.ApiLogger;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

/** Kimi 视觉客户端(OpenAI 兼容):图片 → 多模态模型 → 文字。 */
@Component
public class MoonshotVisionClient implements VisionClient {

    private final HttpClient client;
    private final ObjectMapper mapper;
    private final AppConfig config;

    public MoonshotVisionClient(AppConfig config, ObjectMapper mapper) {
        this.config = config;
        this.mapper = mapper;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(config.timeoutSeconds()))
                .build();
    }

    @Retryable(
            value = TransientApiException.class,
            maxAttemptsExpression = "${llm.maxAttempts:5}",
            backoff = @Backoff(delayExpression = "${llm.retryBaseMs:2000}",
                    maxDelayExpression = "${llm.retryMaxMs:20000}",
                    multiplier = 2.0, random = false))
    @Override
    public String ask(Path image, String prompt) {
        String apiKey = System.getenv("MOONSHOT_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new AgentException("没读到 MOONSHOT_API_KEY —— 先配置环境变量再重启 IDEA");
        }
        try {
            String dataUrl = toDataUrl(image);
            String body = mapper.writeValueAsString(new VisionRequest(
                    config.visionModel(),
                    List.of(new VisionRequest.VisionMessage("user", List.of(
                            VisionRequest.ContentPart.image(dataUrl),
                            VisionRequest.ContentPart.text(prompt)))),
                    config.visionMaxTokens(),
                    VisionRequest.Thinking.DISABLED));      // ← 关思考:OCR 不需要“想”

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.visionApiUrl()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(config.requestTimeoutSeconds()))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            long start = System.currentTimeMillis();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            long cost = System.currentTimeMillis() - start;

            ApiLogger.record(config.visionModel(), response.statusCode(), cost,
                    body.length(), response.body().length());

            int code = response.statusCode();
            if (code == 429 || code >= 500) {
                throw new TransientApiException("Kimi 视觉暂时不可用(" + code + "):" + abbreviate(response.body()));
            }
            if (code < 200 || code >= 300) {
                throw new AgentException("Kimi 视觉返回错误(" + code + "):" + abbreviate(response.body()));
            }

            ChatResponse parsed = mapper.readValue(response.body(), ChatResponse.class);
            if (parsed.choices() == null || parsed.choices().isEmpty()) {
                throw new TransientApiException("Kimi 视觉返回里没有 choices:" + abbreviate(response.body()));
            }
            String content = parsed.choices().get(0).message().content();
            if (content == null || content.isBlank()) {
                // K2.6 偶尔把答案全塞进 reasoning_content、content 留空 → 当瞬时故障重试
                throw new TransientApiException("Kimi 视觉返回空内容(疑似思考模式吞了输出)");
            }
            return content.strip();

        } catch (JsonProcessingException e) {
            throw new AgentException("JSON 处理失败:" + e.getMessage(), e);
        } catch (IOException e) {
            throw new TransientApiException("调用 Kimi 视觉失败(IO):" + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AgentException("调用被中断", e);
        }
    }


    /** 图片 → data URL(base64)。 */
    private String toDataUrl(Path image) throws IOException {
        byte[] bytes = Files.readAllBytes(image);
        String base64 = Base64.getEncoder().encodeToString(bytes);
        return "data:" + mimeOf(image) + ";base64," + base64;
    }

    private static String mimeOf(Path image) {
        String name = image.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".webp")) return "image/webp";
        if (name.endsWith(".gif")) return "image/gif";
        return "image/jpeg";   // .jpg / .jpeg
    }

    private static String abbreviate(String s) {
        if (s == null) return "";
        String one = s.replaceAll("\\s+", " ").strip();
        return one.length() <= 300 ? one : one.substring(0, 300) + "...";
    }
}