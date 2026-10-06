package io.github.likeelysia.formalagent.llm.dto;

import java.util.List;

/**
 * 模型"这一轮"的回话:
 * <ul>
 *   <li>要么是<b>最终答案</b>({@code content});</li>
 *   <li>要么是<b>要求调用工具</b>({@code toolCalls})。</li>
 * </ul>
 */
public record AssistantTurn(String content, List<ToolCall> toolCalls) {

    /** 这一轮是不是要调工具? */
    public boolean wantsTools() {
        return toolCalls != null && !toolCalls.isEmpty();
    }
}
