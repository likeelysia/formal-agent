package io.github.likeelysia.formalagent.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.likeelysia.formalagent.chat.ChatSession;
import io.github.likeelysia.formalagent.chat.Message;
import io.github.likeelysia.formalagent.exception.AgentException;
import io.github.likeelysia.formalagent.service.ChatService;

/**
 * ChatService 的单元测试。
 *
 * <p>关键点:这里用的是 {@link FakeLlmClient} 这个"假客户端" —— 所以整套测试
 * <b>不联网、不烧 token、毫秒级跑完</b>。这就是"面向接口编程"带来的最大好处:
 * 业务逻辑可以脱离真实的外部依赖被验证。
 */
public class ChatServiceTest {

    private ChatSession session;
    private FakeLlmClient fake;
    private ChatService service;

    @BeforeEach
    @DisplayName("每条测试开始前:造一个带 system 人设的会话 + 假客户端(固定回『喵~』)")
    void setUp() {
        session = new ChatSession("test");
        session.add(new Message("system", "你是猫咪饲养员"));
        fake = new FakeLlmClient("喵~");
        service = new ChatService(fake, session);
    }

    @Test
    @DisplayName("send() 之后:历史里多出 user + assistant 两条,顺序和内容都对")
    void sendShouldAppendUserAndAssistant() {
        service.send("你好");

        List<Message> history = session.getMessages();
        assertEquals(3, history.size(), "system + user + assistant = 3 条");

        assertEquals("user", history.get(1).getRole());
        assertEquals("你好", history.get(1).getContent());

        assertEquals("assistant", history.get(2).getRole());
        assertEquals("喵~", history.get(2).getContent());
    }

    @Test
    @DisplayName("send() 的返回值:就是客户端给的那句话")
    void sendShouldReturnClientAnswer() {
        String answer = service.send("你好");
        assertEquals("喵~", answer);
    }

    @Test
    @DisplayName("传给客户端的必须是【快照】:正好 system + user 两条(第 19 行那个坑)")
    void clientShouldReceiveSnapshot() {
        service.send("你好");

        List<Message> received = fake.lastHistory();
        assertNotNull(received, "客户端应当收到过历史");
        assertEquals(2, received.size(), "发出去的历史只应有 system + user(不能把后来的 assistant 也带进去)");
        assertEquals("system", received.get(0).getRole());
        assertEquals("你好", received.get(1).getContent());
    }

    @Test
    @DisplayName("客户端抛 AgentException 时,send() 要如实往外抛(不吞异常)")
    void clientFailureShouldPropagate() {
        // LlmClient 只有一个抽象方法 → 可以直接用 lambda 造一个"必定失败"的实现
        LlmClient failing = (history, options) -> {
            throw new AgentException("网络炸了");
        };
        ChatService svc = new ChatService(failing, new ChatSession("t2"));

        AgentException e = assertThrows(AgentException.class, () -> svc.send("在吗"));
        assertEquals("网络炸了", e.getMessage());
    }
}
