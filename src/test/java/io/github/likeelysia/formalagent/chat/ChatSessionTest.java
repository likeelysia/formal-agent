package io.github.likeelysia.formalagent.chat;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Serializable;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * ChatSession 的单元测试:重点看「历史怎么加、顺序对不对」。
 *
 * <p>@BeforeEach 的用处:每个 @Test 跑之前都会先执行它一次,
 * 于是**每条测试都从一个干净的新会话开始**,互不干扰(测试之间不能有依赖)。
 */
class ChatSessionTest {

    /** 每个测试共用的被测对象(由 @BeforeEach 准备) */
    private ChatSession session;

    @BeforeEach
    @DisplayName("每条测试开始前:新建一个干净会话")
    void setUp() {
        session = new ChatSession("test-session");
    }

    @Test
    @DisplayName("新建的会话:名字对、历史为空")
    void newSessionShouldBeEmpty() {
        assertEquals("test-session", session.getName());
        assertTrue(session.getMessages().isEmpty(), "刚创建时历史应当是空的");
    }

    @Test
    @DisplayName("add() 之后:条数对、顺序对")
    void addShouldAppendInOrder() {
        session.add(new Message("system", "人设"));
        session.add(new Message("user", "你好"));
        session.add(new Message("assistant", "喵~"));

        List<Message> history = session.getMessages();

        assertEquals(3, history.size(), "加了 3 条就应该是 3 条");
        assertEquals("system", history.get(0).getRole(), "第 1 条应当是 system");
        assertEquals("你好", history.get(1).getContent(), "第 2 条应当是用户那句话");
        assertEquals("assistant", history.get(2).getRole(), "第 3 条应当是 assistant");
    }

    @Test
    @DisplayName("printHistory():空历史 / 有历史都不该抛异常")
    void printHistoryShouldNotThrow() {
        // 空历史
        assertDoesNotThrow(() -> session.printHistory());

        // 有历史
        session.add(new Message("user", "有内容了"));
        assertDoesNotThrow(() -> session.printHistory());
    }

    @Test
    @DisplayName("ChatSession 必须实现 Serializable —— 它是要整体写进 .bin 的对象")
    void sessionShouldBeSerializable() {
        assertTrue(session instanceof Serializable);
    }
}
