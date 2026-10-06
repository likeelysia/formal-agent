package io.github.likeelysia.formalagent.llm.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/** 模型请求调用某个工具(OpenAI / DeepSeek 兼容格式)。 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ToolCall(String id, String type, Function function) {

    /**
     * 工具名 + 参数。
     *
     * <p>注意:{@code arguments} 是一段 <b>JSON 字符串</b>(不是 JSON 对象)—— 这是 API 约定,
     * 所以调用方得自己再 {@code mapper.readValue(...)} 解析一次。
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Function(String name, String arguments) {
    }
}
