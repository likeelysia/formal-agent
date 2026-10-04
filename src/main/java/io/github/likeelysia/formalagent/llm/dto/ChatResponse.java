package io.github.likeelysia.formalagent.llm.dto;

import java.util.List;

/**
 * 一次「对话补全」响应体。
 *
 * <p>只声明我们真正关心的字段(choices → message → content),
 * 其余字段(id / usage / finish_reason 等)靠 ObjectMapper 的
 * {@code FAIL_ON_UNKNOWN_PROPERTIES=false} 自动忽略 —— 模型 API 经常新增字段,
 * 不该因为"多认识一个字段"就整体解析失败。
 *
 * <p>嵌套 record 正好对应 JSON 的嵌套结构,取代过去
 * {@code readTree().path("choices").path(0).path("message").path("content")} 的裸导航。
 */
public record ChatResponse(List<Choice> choices) {

    public record Choice(ChatMessage message) {
    }

    public record ChatMessage(String role, String content) {
    }
}
