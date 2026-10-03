package io.github.likeelysia.formalagent.exception;

/** 项目统一异常:网络/解析/配置等问题,统一抛它。 */
public class AgentException extends RuntimeException {          // ← 继承 RuntimeException = 非受检
    public AgentException(String message) { super(message); }
    public AgentException(String message, Throwable cause) { super(message, cause); }  // 保留原始异常
}
