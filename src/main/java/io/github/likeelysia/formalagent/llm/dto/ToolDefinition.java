package io.github.likeelysia.formalagent.llm.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

/**
 * 一个"可被模型调用的工具"的声明:名字 + 说明 + 参数 JSON Schema。
 *
 * <p>关键点:{@code description} 就是给模型看的<b>提示词</b> —— 它决定模型什么时候会想起来用你
 * 这个工具。写不清,模型就永远不会调它。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ToolDefinition(String type, Function function) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Function(String name, String description, Map<String, Object> parameters) {
    }

    public static ToolDefinition of(String name, String description, Map<String, Object> parameters) {
        return new ToolDefinition("function", new Function(name, description, parameters));
    }
}
