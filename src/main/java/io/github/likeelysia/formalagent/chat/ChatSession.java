package io.github.likeelysia.formalagent.chat;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 一次会话:一个名字 + 一串消息(= 多轮对话的历史)。
 */
public class ChatSession implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String name;
    private final List<Message> messages = new ArrayList<>();

    public ChatSession(String name) {
        this.name = name;
    }

    public String getName() { return name; }

    public List<Message> getMessages() { return messages; }

    /** 往历史里追加一条 */
    public void add(Message message) {
        messages.add(message);
    }

    /** 打印历史(自己排查用) */
    public void printHistory() {
        if (messages.isEmpty()) {
            System.out.println("  (暂无历史)");
            return;
        }
        for (Message m : messages) {
            System.out.println("  " + m);
        }
    }
}