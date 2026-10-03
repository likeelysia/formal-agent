package io.github.likeelysia.formalagent.llm;

/** 大模型客户端:只回答一件事 —— "给我对话历史,还我模型回复"。 */
public interface LlmClient {
    String chat(java.util.List<io.github.likeelysia.formalagent.chat.Message> history);
}
