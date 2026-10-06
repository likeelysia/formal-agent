package io.github.likeelysia.formalagent.llm.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Kimi 视觉请求体(多模态 content 数组)。 */
public record VisionRequest(
        String model,
        List<VisionMessage> messages,
        @JsonProperty("max_tokens") int maxTokens,
        Thinking thinking) {

    /**
     * 思考开关(kimi-k2.6 支持 type = "enabled"/"disabled")。
     * OCR 是“照抄”任务:关掉思考既能提速减费,又能避开“答案全跑进 reasoning_content、content 留空”的抽风。
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Thinking(String type) {
        public static final Thinking DISABLED = new Thinking("disabled");
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record VisionMessage(String role, List<ContentPart> content) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ContentPart(String type, String text,
                              @JsonProperty("image_url") ImageUrl imageUrl) {
        public static ContentPart text(String t) {
            return new ContentPart("text", t, null);
        }

        public static ContentPart image(String dataUrl) {
            return new ContentPart("image_url", null, new ImageUrl(dataUrl));
        }
    }

    public record ImageUrl(String url) {
    }
}