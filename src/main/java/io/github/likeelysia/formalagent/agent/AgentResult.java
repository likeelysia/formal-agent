package io.github.likeelysia.formalagent.agent;

import java.util.List;

/** 一次 Agent 运行的结果:最终答案 + 工具调用轨迹(trace)+ 用了几步。 */
public record AgentResult(String answer, List<String> trace, int steps) {
}
