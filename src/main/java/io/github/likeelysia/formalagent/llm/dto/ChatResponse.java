package io.github.likeelysia.formalagent.llm.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * 一次「对话补全」响应体。
 *
 * <p>只声明我们真正关心的字段(choices → message → content / tool_calls),
 * 其余字段(id / usage / finish_reason 等)靠 ObjectMapper 的
 * {@code FAIL_ON_UNKNOWN_PROPERTIES=false} 自动忽略 —— 模型 API 经常新增字段,
 * 不该因为"多认识一个字段"就整体解析失败。
 */
public record ChatResponse(List<Choice> choices) {

    public record Choice(ChatMessage message) {
    }

    /** assistant 的消息:普通对话只有 content;要求调工具时带着 tool_calls。 */
    public record ChatMessage(
            String role,
            String content,
            @JsonProperty("tool_calls") List<ToolCall> toolCalls) {
    }
}
