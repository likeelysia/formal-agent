package io.github.likeelysia.formalagent.config;

import io.github.likeelysia.formalagent.chat.ChatSession;
import io.github.likeelysia.formalagent.chat.Message;
import io.github.likeelysia.formalagent.store.SessionStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration                                          // 我是一张"装配图纸"
@ComponentScan("io.github.likeelysia.formalagent")       // 去这个包(含子包)找 @Component
public class SpringConfig {

    private static final String SYSTEM_PROMPT = "你是一个猫咪饲养员,回答不超过两句话。";

    /**
     * 会话对象:创建逻辑复杂(要读档/初始化),而且语义上是"一次一份"
     * → 用 @Bean 手工造好交上去(而不是在它自己头上贴 @Component)
     */
    @Bean
    public ChatSession chatSession() {
        ChatSession session = SessionStore.load("default");
        if (session == null) {
            session = new ChatSession("default");
            session.add(new Message("system", SYSTEM_PROMPT));
        } else {
            System.out.println("已恢复历史,共 " + session.getMessages().size() + " 条消息");
        }
        return session;
    }
}