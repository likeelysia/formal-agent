package io.github.likeelysia.formalagent.chat;

import java.io.Serializable;

/**
 * 一条对话消息(角色 + 内容)。
 * 实现 Serializable 之后,整个对象才能被"序列化"写进文件(B2 要用)。
 */
public class Message implements Serializable {

    /** 序列化版本号:显式写死 —— 以后给这个类加字段,老的会话文件也不会读不了 */
    private static final long serialVersionUID = 1L;

    private final String role;      // system / user / assistant
    private final String content;

    public Message(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public String getRole()    { return role; }
    public String getContent() { return content; }

    @Override
    public String toString() {
        return role + ": " + content;
    }
}