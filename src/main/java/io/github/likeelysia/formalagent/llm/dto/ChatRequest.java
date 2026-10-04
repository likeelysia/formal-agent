package io.github.likeelysia.formalagent.llm.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.likeelysia.formalagent.chat.Message;
import java.util.List;

/**
 * 一次「对话补全」请求体(对应 DeepSeek / OpenAI 的 /chat/completions 入参)。
 *
 * <p>用 record 把请求体做成<b>强类型对象</b>,交给 Jackson 序列化 ——
 * 取代过去"手拼 JSON 字符串"的写法:字段名/类型在编译期即可校验,
 * 不会因为少一个引号、多一个换行就产出非法 JSON。
 *
 * <p>为什么用 {@code @JsonProperty("max_tokens")}:Java 习惯驼峰 {@code maxTokens},
 * 但 API 要下划线 {@code max_tokens},靠注解显式映射,比全局命名策略更安全(不影响其它字段)。
 */
public record ChatRequest(
        String model,
        List<Message> messages,
        @JsonProperty("max_tokens") int maxTokens) {
}
