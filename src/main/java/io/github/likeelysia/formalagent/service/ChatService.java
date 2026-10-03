package io.github.likeelysia.formalagent.service;

import io.github.likeelysia.formalagent.chat.ChatSession;
import io.github.likeelysia.formalagent.chat.Message;
import io.github.likeelysia.formalagent.llm.LlmClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ChatService {
    private final LlmClient client;        // ← 只认接口
    private final ChatSession session;

    @Autowired
    public ChatService(LlmClient client, ChatSession session) {   // ★ 依赖从"外面传进来"
        this.client = client;
        this.session = session;
    }

    /** 说一句 → 调模型 → 两边都记进历史 */
    public String send(String userInput) {
        session.add(new Message("user", userInput));
        String answer = client.chat(List.copyOf(session.getMessages()));
        session.add(new Message("assistant", answer));
        return answer;
    }

    public ChatSession getSession() { return session; }
}
