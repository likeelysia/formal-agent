package io.github.likeelysia.formalagent.llm.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.likeelysia.formalagent.chat.Message;
import java.util.List;

/**
 * 发给模型的"一条消息"(工具调用场景的完整版)。
 *
 * <p>为什么要单独一个类型:工具调用里,assistant 消息要带 {@code tool_calls}、
 * 工具结果消息要带 {@code tool_call_id} —— 这些是<b>网络协议细节</b>,
 * 不该污染业务侧的 {@link Message}(它还要被序列化进会话存档)。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AgentMessage(
        String role,
        String content,
        @JsonProperty("tool_calls") List<ToolCall> toolCalls,
        @JsonProperty("tool_call_id") String toolCallId) {

    /** 业务侧 {@link Message} → 线上格式。 */
    public static AgentMessage of(Message message) {
        return new AgentMessage(message.getRole(), message.getContent(), null, null);
    }

    public static AgentMessage system(String content) {
        return new AgentMessage("system", content, null, null);
    }

    public static AgentMessage user(String content) {
        return new AgentMessage("user", content, null, null);
    }

    /** assistant 这一轮"我要调这些工具"(content 可以为空)。 */
    public static AgentMessage assistantWithTools(List<ToolCall> calls) {
        return new AgentMessage("assistant", null, calls, null);
    }

    /** 工具执行结果(必须带上被回应的 {@code tool_call_id},否则 API 会报错)。 */
    public static AgentMessage tool(String toolCallId, String content) {
        return new AgentMessage("tool", content, null, toolCallId);
    }
}
