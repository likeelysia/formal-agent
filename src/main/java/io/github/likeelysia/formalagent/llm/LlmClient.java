package io.github.likeelysia.formalagent.llm;

import io.github.likeelysia.formalagent.chat.Message;
import io.github.likeelysia.formalagent.exception.AgentException;
import io.github.likeelysia.formalagent.llm.dto.AgentMessage;
import io.github.likeelysia.formalagent.llm.dto.AssistantTurn;
import io.github.likeelysia.formalagent.llm.dto.ToolDefinition;
import java.util.List;

/** 大模型客户端:给我对话历史,还我模型回复。 */
public interface LlmClient {

    /** 带调参的对话(主方法)。 */
    String chat(List<Message> history, ChatOptions options);

    /** 普通对话的便捷重载:走默认参数。 */
    default String chat(List<Message> history) {
        return chat(history, ChatOptions.DEFAULT);
    }

    /**
     * 带工具的对话(Agent 用):把完整消息(含工具执行结果)发出去,拿回"模型这一轮"的回复
     * —— 它可能给最终答案,也可能要求再调工具。
     *
     * <p>不支持工具的实现不必覆盖(默认抛异常)。
     */
    default AssistantTurn chatWithTools(List<AgentMessage> messages, List<ToolDefinition> tools) {
        throw new AgentException("当前 LlmClient 实现不支持工具调用");
    }
}
