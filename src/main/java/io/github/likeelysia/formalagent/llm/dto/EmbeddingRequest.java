package io.github.likeelysia.formalagent.llm.dto;

import java.util.List;

/** OpenAI 兼容的 embedding 请求体:{"model":"...","input":["文本1","文本2",...]}。 */
public record EmbeddingRequest(String model, List<String> input) {
}
