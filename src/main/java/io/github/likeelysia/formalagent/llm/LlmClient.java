package io.github.likeelysia.formalagent.llm;

import io.github.likeelysia.formalagent.chat.Message;
import java.util.List;

/** 大模型客户端:给我对话历史(+ 可选参数),还我模型回复。 */
public interface LlmClient {

    /** 带调参的对话(主方法)。 */
    String chat(List<Message> history, ChatOptions options);

    /** 普通对话的便捷重载:走默认参数。 */
    default String chat(List<Message> history) {
        return chat(history, ChatOptions.DEFAULT);
    }
}