package io.github.likeelysia.formalagent.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;

/** 验证 @Retryable(AOP 重试切面)确实生效:靠一个"前两次必失败、第三次成功"的测试 bean。 */
class RetryBehaviorTest {

    @TestConfiguration          // 用 @TestConfiguration:不会被 @SpringBootTest 的组件扫描挑中(否则同名 bean 冲突)
    @EnableRetry
    static class RetryTestConfig {
        @Bean
        public FlakyService flakyService() { return new FlakyService(); }
    }

    public static class FlakyService {
        private final AtomicInteger calls = new AtomicInteger(0);
        private final AtomicInteger badCalls = new AtomicInteger(0);

        @Retryable(value = IllegalStateException.class, maxAttempts = 3,
                backoff = @Backoff(delay = 1))       // 测试把延迟压到 1ms,别真等
        public String call() {
            int n = calls.incrementAndGet();
            if (n < 3) throw new IllegalStateException("第 " + n + " 次失败");
            return "ok";
        }

        @Retryable(value = IllegalStateException.class, maxAttempts = 3,
                backoff = @Backoff(delay = 1))
        public String alwaysBad() {
            badCalls.incrementAndGet();
            throw new IllegalArgumentException("业务错误,不该重试");
        }

        public int calls()    { return calls.get(); }
        public int badCalls() { return badCalls.get(); }
    }

    @Test
    @DisplayName("失败两次后成功 → AOP 重试生效(共调用 3 次)")
    void retriesUntilSuccess() {
        try (AnnotationConfigApplicationContext ctx =
                     new AnnotationConfigApplicationContext(RetryTestConfig.class)) {
            FlakyService service = ctx.getBean(FlakyService.class);

            assertEquals("ok", service.call());
            assertEquals(3, service.calls());
        }
    }

    @Test
    @DisplayName("非 retryFor 的异常 → 只调用 1 次(不重试)")
    void doesNotRetryOtherExceptions() {
        try (AnnotationConfigApplicationContext ctx =
                     new AnnotationConfigApplicationContext(RetryTestConfig.class)) {   // 复用它即可
            FlakyService service = ctx.getBean(FlakyService.class);

            assertThrows(IllegalArgumentException.class, service::alwaysBad);
            assertEquals(1, service.badCalls());
        }
    }
}