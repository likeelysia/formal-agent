package io.github.likeelysia.formalagent.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.likeelysia.formalagent.chat.ChatSession;
import io.github.likeelysia.formalagent.llm.LlmClient;
import io.github.likeelysia.formalagent.service.ChatService;
import io.github.likeelysia.formalagent.store.SessionStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Spring 装配的「冒烟测试」:容器能起来、关键 bean 齐备、共享依赖是单例。
 *
 * <p>为什么要它:单元测试是各自 new 对象,<b>测不到"容器接线"</b>。
 * 一旦构造函数改了却忘了让容器注入(比如这次 SessionStore 由静态类改 bean),
 * 只有真启动一次容器才会暴露 —— 这个测试就是干这个的,不联网、秒级。
 */
class SpringConfigTest {

    @Test
    @DisplayName("容器能启动,关键 bean 齐备,ObjectMapper 是单例")
    void contextWiresUp() {
        try (AnnotationConfigApplicationContext ctx =
                     new AnnotationConfigApplicationContext(SpringConfig.class)) {

            assertNotNull(ctx.getBean(ObjectMapper.class), "ObjectMapper bean 应存在");
            assertNotNull(ctx.getBean(SessionStore.class), "SessionStore bean 应存在");
            assertNotNull(ctx.getBean(ChatService.class), "ChatService bean 应存在");
            assertNotNull(ctx.getBean(ChatSession.class), "ChatSession bean 应存在");
            assertNotNull(ctx.getBean(LlmClient.class), "LlmClient 实现应存在");

            // 同一份 ObjectMapper:注入到 DeepSeekClient / KnowledgeExtractor / SessionStore 的是同一个实例
            assertSame(ctx.getBean(ObjectMapper.class), ctx.getBean(ObjectMapper.class),
                    "ObjectMapper 必须是单例");
        }
    }
}
