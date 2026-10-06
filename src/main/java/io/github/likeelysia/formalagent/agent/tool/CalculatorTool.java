package io.github.likeelysia.formalagent.agent.tool;

import io.github.likeelysia.formalagent.agent.Tool;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** 工具:计算数学表达式(模型自己算容易出错,交给它)。内部走安全的自写求值器。 */
@Component
public class CalculatorTool implements Tool {

    @Override
    public String name() {
        return "calculator";
    }

    @Override
    public String description() {
        return "计算一个数学表达式。支持四则运算 + - * / %、乘方 ^、括号,以及函数 "
                + "sqrt/abs/sin/cos/tan/asin/acos/atan/ln/log/exp/pow/min/max,常量 pi、e。"
                + "三角函数用弧度。当你需要做数值计算时使用它 —— 不要自己心算。";
    }

    @Override
    public Map<String, Object> parameters() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "expression", Map.of("type", "string",
                                "description", "要计算的数学表达式,例如 \"3e8 * 2\" 或 \"sqrt(2) + 1\"")),
                "required", List.of("expression"));
    }

    @Override
    public String execute(Map<String, Object> args) {
        String expression = Tool.strArg(args, "expression");
        if (expression.isBlank()) return "expression 不能为空";
        double value = ExpressionEvaluator.eval(expression);   // 出错就抛 → AgentService 变成"观察结果"
        return expression + " = " + Tool.num(value);
    }
}
