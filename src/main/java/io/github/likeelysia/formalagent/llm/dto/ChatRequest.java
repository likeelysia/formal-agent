package io.github.likeelysia.formalagent.llm.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.likeelysia.formalagent.chat.Message;
import java.util.List;

/**
 * 一次「对话补全」请求体。
 *
 * <p>{@code @JsonInclude(NON_NULL)}:当 {@code responseFormat} 为 null(普通对话)时不序列化该字段,
 * 避免发出 {@code "response_format": null} 这种多余/可能非法的内容。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatRequest(
        String model,
        List<Message> messages,
        @JsonProperty("max_tokens") int maxTokens,
        @JsonProperty("response_format") ResponseFormat responseFormat) {

    /** 结构化输出开关:{"type":"json_object"}。 */
    public record ResponseFormat(String type) {
        /** 要求模型只输出合法 JSON。 */
        public static final ResponseFormat JSON = new ResponseFormat("json_object");
    }
}