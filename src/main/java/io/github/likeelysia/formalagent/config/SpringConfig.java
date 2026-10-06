package io.github.likeelysia.formalagent.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.likeelysia.formalagent.chat.ChatSession;
import io.github.likeelysia.formalagent.chat.Message;
import io.github.likeelysia.formalagent.store.SessionStore;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry(proxyTargetClass = true)   // 启用重试切面(CGLIB 代理,注解更好使)
@EnableConfigurationProperties(AppConfig.class)  // 把 fa.* 绑定成 AppConfig bean
@Configuration                                          // 我是一张"装配图纸"
@ComponentScan("io.github.likeelysia.formalagent")       // 去这个包(含子包)找 @Component
public class SpringConfig {

    private static final String SYSTEM_PROMPT = "你是一个猫咪饲养员,回答不超过两句话。";

    /**
     * 全项目共享的 JSON 序列化器(@Bean = 容器里唯一一份)。
     *
     * <p>统一在这里配置,所有用到 JSON 的地方都注入它,而不是各自 {@code new ObjectMapper()}。
     * 目前只加一条:忽略 JSON 里不认识的字段 —— 模型 API 常新增字段,
     * 不该因此让整次解析失败。
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * 会话对象:创建逻辑复杂(要读档/初始化),而且语义上是"一次一份"
     * → 用 @Bean 手工造好交上去(而不是在它自己头上贴 @Component)。
     *
     * <p>读档依赖 SessionStore,直接作为方法参数写出来 → 由容器注入,
     * 不再手写 {@code SessionStore.load(...)} 这种"自己找依赖"的静态调用。
     */
    @Bean
    public ChatSession chatSession(SessionStore store) {
        ChatSession session = store.load("default");
        if (session == null) {
            session = new ChatSession("default");
            session.add(new Message("system", SYSTEM_PROMPT));
        } else {
            System.out.println("已恢复历史,共 " + session.getMessages().size() + " 条消息");
        }
        return session;
    }
}
