package io.github.likeelysia.formalagent.llm;

import io.github.likeelysia.formalagent.exception.AgentException;

/**
 * 「瞬时故障」异常:只有网络抖动 / 限流(429)/ 服务端 5xx 才抛它。
 * 特意做成独立子类 —— 好让 @Retryable 只重试这一类,
 * 而业务错误(参数错、鉴权失败)不重试。
 */
public class TransientApiException extends AgentException {
    public TransientApiException(String message) { super(message); }
    public TransientApiException(String message, Throwable cause) { super(message, cause); }
}