package io.github.likeelysia.formalagent.llm.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * 一次「对话补全」请求体。
 *
 * <p>{@code @JsonInclude(NON_NULL)}:字段为 null 时不序列化 —— 普通对话不会带上
 * {@code response_format} / {@code tools} 这些多余字段。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatRequest(
        String model,
        List<AgentMessage> messages,
        @JsonProperty("max_tokens") int maxTokens,
        @JsonProperty("response_format") ResponseFormat responseFormat,
        List<ToolDefinition> tools) {

    /** 结构化输出开关:{"type":"json_object"}。 */
    public record ResponseFormat(String type) {
        /** 要求模型只输出合法 JSON。 */
        public static final ResponseFormat JSON = new ResponseFormat("json_object");
    }
}
