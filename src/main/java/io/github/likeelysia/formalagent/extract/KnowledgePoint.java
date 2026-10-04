package io.github.likeelysia.formalagent.extract;

/**
 * 一个知识点:名称 + 一句话说明。
 *
 * <p>record 是 Java 17 的"极简不可变数据类":一行就自动获得
 * 构造器、访问器、equals/hashCode/toString。
 *
 * <p>Jackson 反序列化 JSON 数组时会按"括号里的名字"自动配对:
 * {"name": "...", "detail": "..."} → new KnowledgePoint(name, detail)。
 */
public record KnowledgePoint(String name, String detail) {
}