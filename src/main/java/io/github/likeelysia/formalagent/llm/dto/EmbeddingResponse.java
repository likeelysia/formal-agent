package io.github.likeelysia.formalagent.llm.dto;

import java.util.List;

/**
 * OpenAI 兼容的 embedding 响应体:{"data":[{"embedding":[...]}, ...]}。
 *
 * <p>只声明我们关心的 data[].embedding;其余字段(id/usage 等)被 ObjectMapper 忽略。
 */
public record EmbeddingResponse(List<Item> data) {

    /** 一条向量结果。Jackson 会把 JSON 数组直接映射成 float[]。 */
    public record Item(float[] embedding) {
    }
}
