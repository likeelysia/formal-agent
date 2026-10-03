package io.github.likeelysia.formalagent.llm;

import io.github.likeelysia.formalagent.chat.Message;

import java.util.List;

/** 测试替身:不联网,固定返回一句话,还能记住"业务层传了什么历史给我" */
class FakeLlmClient implements LlmClient {
    private final String answer;
    private List<Message> lastHistory;
    FakeLlmClient(String answer) { this.answer = answer; }

    @Override public String chat(List<Message> history) {
        this.lastHistory = history;
        return answer;
    }
    List<Message> lastHistory() { return lastHistory; }
}
