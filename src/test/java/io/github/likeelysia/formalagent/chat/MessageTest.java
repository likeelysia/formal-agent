package io.github.likeelysia.formalagent.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Serializable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Message 的单元测试。
 *
 * <p>读法提示:每个 @Test 方法就是"一条检查规则",三段式 = 准备 → 执行 → 断言。
 * 这个类只测 Message 自己的行为,不碰网络、不碰文件 → 跑得飞快(毫秒级)。
 */
class MessageTest {

    @Test
    @DisplayName("构造之后,role / content 能原样取出来")
    void gettersShouldReturnConstructorArgs() {
        // ① 准备
        Message m = new Message("user", "你好");

        // ② 断言(期望值在前,实际值在后)
        assertEquals("user", m.getRole());
        assertEquals("你好", m.getContent());
    }

    @Test
    @DisplayName("toString() 的格式是『角色: 内容』")
    void toStringShouldBeRoleColonContent() {
        Message m = new Message("assistant", "喵~");
        assertEquals("assistant: 喵~", m.toString());
    }

    @Test
    @DisplayName("内容里带换行和冒号时,toString() 也不该崩(边界值)")
    void toStringShouldSurviveWeirdContent() {
        Message m = new Message("user", "第一行\n第二行: 带冒号");
        String s = m.toString();

        assertTrue(s.startsWith("user: "), "应当以『角色 + 冒号空格』开头");
        assertTrue(s.contains("第一行"), "应当保留原始内容");
    }

    @Test
    @DisplayName("Message 必须实现 Serializable —— 因为 SessionStore 要把它写进 .bin")
    void messageShouldBeSerializable() {
        // 这条不是测"功能",而是把"契约"钉住:以后谁把 implements Serializable 删了,这里立刻红灯
        assertTrue(new Message("user", "x") instanceof Serializable);
    }
}
