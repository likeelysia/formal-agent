package io.github.likeelysia.formalagent.agent;

import io.github.likeelysia.formalagent.knowledge.KnowledgeItem;
import io.github.likeelysia.formalagent.llm.dto.ToolDefinition;
import java.util.List;
import java.util.Map;

/**
 * 一个"工具":模型可以请求调用它,由我们来执行,再把结果喂回去。
 *
 * <p>设计要点:
 * <ul>
 *   <li>{@link #description()} 就是给模型看的<b>提示词</b> —— 写清楚"什么时候该用它",否则模型不会想起它;</li>
 *   <li>{@link #parameters()} 用 JSON Schema 描述参数,能让模型少填错;</li>
 *   <li>{@link #execute(Map)} 收参数、返回一段文本(作为"观察结果");出错请抛异常,由 AgentService 兜住。</li>
 * </ul>
 */
public interface Tool {

    String name();

    String description();

    /** 参数 JSON Schema,形如 {"type":"object","properties":{...},"required":[...]}。 */
    Map<String, Object> parameters();

    /** 执行工具;返回给模型看的文本。 */
    String execute(Map<String, Object> args);

    /** 转成模型 API 要的工具声明。 */
    default ToolDefinition definition() {
        return ToolDefinition.of(name(), description(), parameters());
    }

    // ---------------------------------------------------------------- 共用小工具

    /** 取字符串参数(模型偶尔会把数字/别的类型塞进来,这里统一转一下)。 */
    static String strArg(Map<String, Object> args, String key) {
        Object v = args == null ? null : args.get(key);
        return v == null ? "" : String.valueOf(v).strip();
    }

    /** 取整数参数;缺省或格式不对时返回默认值。 */
    static int intArg(Map<String, Object> args, String key, int defaultValue) {
        Object v = args == null ? null : args.get(key);
        if (v instanceof Number number) return number.intValue();
        if (v instanceof String s) {
            try {
                return Integer.parseInt(s.strip());
            } catch (NumberFormatException ignored) {
                // 落到默认值
            }
        }
        return defaultValue;
    }

    /** 取小数参数;缺省或格式不对时抛异常(由 AgentService 变成“观察结果”还给模型)。 */
    static double doubleArg(Map<String, Object> args, String key) {
        Object v = args == null ? null : args.get(key);
        if (v instanceof Number number) return number.doubleValue();
        if (v instanceof String s) {
            try {
                return Double.parseDouble(s.strip());
            } catch (NumberFormatException ignored) {
                // 落到下面的异常
            }
        }
        throw new IllegalArgumentException(key + " 必须是数字");
    }

    /** 数字格式化:整数就显示整数,否则保留 6 位有效数字(且不补多余零)。 */
    static String num(double v) {
        if (!Double.isFinite(v)) return String.valueOf(v);
        if (v == Math.rint(v) && Math.abs(v) < 1e15) return String.valueOf((long) v);
        return new java.math.BigDecimal(v)
                .round(new java.math.MathContext(6))
                .stripTrailingZeros()
                .toPlainString();
    }

    /** 把知识点格式化成给模型看的文本(统一带出处)。 */
    static String format(List<KnowledgeItem> items) {
        if (items == null || items.isEmpty()) return "(没有检索到相关知识点)";
        StringBuilder sb = new StringBuilder();
        for (KnowledgeItem k : items) {
            sb.append("- ").append(k.name()).append(":").append(k.detail())
                    .append("(出处:").append(k.source())
                    .append(k.location() == null ? "" : " " + k.location()).append(")\n");
        }
        return sb.toString();
    }
}
