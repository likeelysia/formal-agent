package io.github.likeelysia.formalagent.extract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.likeelysia.formalagent.chat.Message;
import io.github.likeelysia.formalagent.exception.AgentException;
import io.github.likeelysia.formalagent.llm.LlmClient;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * KnowledgeExtractor 的单元测试。
 * 用的是打桩的"假客户端" —— 所以这一套测试不联网、不烧 token、毫秒级跑完。
 */
class KnowledgeExtractorTest {

    /** 测试替身:固定返回我们准备好的字符串,顺便记住"业务层到底发了什么历史给我"。 */
    private static class FakeLlmClient implements LlmClient {
        private final String answer;
        private List<Message> lastHistory;
        private int calls;

        FakeLlmClient(String answer) { this.answer = answer; }

        @Override public String chat(List<Message> history) {
            this.lastHistory = history;
            this.calls++;
            return answer;
        }
    }

    @Test
    @DisplayName("正常 JSON → 解析出两个知识点,字段都对")
    void parsesPlainJson() {
        FakeLlmClient fake = new FakeLlmClient(
                "[{\"name\":\"IoC\",\"detail\":\"控制反转,把创建对象的权力交给容器\"},"
                        + "{\"name\":\"DI\",\"detail\":\"依赖注入,容器把依赖塞进对象\"}]");

        List<KnowledgePoint> points = new KnowledgeExtractor(fake).extract("IoC 就是控制反转……");

        assertEquals(2, points.size());
        assertEquals("IoC", points.get(0).name());
        assertEquals("控制反转,把创建对象的权力交给容器", points.get(0).detail());
    }

    @Test
    @DisplayName("模型加 ```json 围栏 + 客套话 → 洗掉之后照样能解析")
    void stripsFencesAndChatter() {
        FakeLlmClient fake = new FakeLlmClient(
                "好的,结果如下:\n```json\n[{\"name\":\"注解\",\"detail\":\"给代码贴的标签\"}]\n```\n希望有帮助!");

        List<KnowledgePoint> points = new KnowledgeExtractor(fake).extract("注解是……");

        assertEquals(1, points.size());
        assertEquals("注解", points.get(0).name());
    }

    @Test
    @DisplayName("空白文本 → 返回空列表,并且一次请求都不发")
    void blankChunkSendsNothing() {
        FakeLlmClient fake = new FakeLlmClient("[]");

        assertTrue(new KnowledgeExtractor(fake).extract("   \n  ").isEmpty());
        assertEquals(0, fake.calls);
    }

    @Test
    @DisplayName("模型返回空数组 [] → 空列表(不是 null)")
    void emptyArrayGivesEmptyList() {
        List<KnowledgePoint> points = new KnowledgeExtractor(new FakeLlmClient("[]")).extract("目录:第一章");

        assertTrue(points.isEmpty());
    }

    @Test
    @DisplayName("模型胡说八道(不是 JSON)→ 抛 AgentException,报错里带原始返回")
    void badResponseThrows() {
        FakeLlmClient fake = new FakeLlmClient("抱歉,我无法完成这个请求。");

        AgentException e = assertThrows(AgentException.class, () -> new KnowledgeExtractor(fake).extract("随便一段"));

        assertTrue(e.getMessage().contains("抱歉"), "报错信息里应该带上模型的原始返回,方便排查");
    }

    @Test
    @DisplayName("发给模型的是两条消息:system 提示词 + user 文本块")
    void sendsSystemAndUser() {
        FakeLlmClient fake = new FakeLlmClient("[]");

        new KnowledgeExtractor(fake).extract("第一段内容");

        assertEquals(2, fake.lastHistory.size());
        assertEquals("system", fake.lastHistory.get(0).getRole());
        assertEquals("user", fake.lastHistory.get(1).getRole());
        assertEquals("第一段内容", fake.lastHistory.get(1).getContent());
    }
}