package io.github.likeelysia.formalagent.agent.tool;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 极简的数学表达式求值器(递归下降),专为"给模型算数"设计。
 *
 * <p><b>为什么要自己写、而不是 eval / 引库</b>:表达式是模型给的(模型又可能被用户输入影响),
 * 所以求值必须是<b>安全</b>的 —— 这里只认:
 * <ul>
 *   <li>数字(含小数与科学计数法,如 {@code 3e8});</li>
 *   <li>运算符 {@code + - * / %} 与乘方 {@code ^},括号;</li>
 *   <li>白名单函数:{@code sqrt abs sin cos tan asin acos atan ln log exp pow min max};</li>
 *   <li>常量:{@code pi}、{@code e}。</li>
 * </ul>
 * <b>不认变量、不认任何方法调用</b> —— 于是 {@code os.system(...)} 这种根本解析不过去。
 *
 * <p>文法(优先级从低到高):
 * <pre>
 *   expression := term (('+'|'-') term)*
 *   term       := unary (('*'|'/'|'%') unary)*
 *   unary      := ('+'|'-') unary | power
 *   power      := primary ('^' unary)?          // 右结合
 *   primary    := number | '(' expression ')' | ident '(' args ')' | ident
 * </pre>
 */
final class ExpressionEvaluator {

    /** 只允许的常量(没有变量 —— 这是安全的关键)。 */
    private static final Map<String, Double> CONSTANTS = Map.of("pi", Math.PI, "e", Math.E);

    private final String s;
    private int i;

    private ExpressionEvaluator(String s) {
        this.s = s;
    }

    static double eval(String expression) {
        if (expression == null || expression.isBlank()) {
            throw new IllegalArgumentException("表达式为空");
        }
        ExpressionEvaluator evaluator = new ExpressionEvaluator(expression);
        double value = evaluator.parseExpression();
        evaluator.skipSpaces();
        if (evaluator.i < expression.length()) {
            throw new IllegalArgumentException("表达式里有无法识别的内容:" + expression.substring(evaluator.i));
        }
        return value;
    }

    // ---------------------------------------------------------------- 文法

    private double parseExpression() {
        double value = parseTerm();
        while (true) {
            skipSpaces();
            if (eat('+')) value += parseTerm();
            else if (eat('-')) value -= parseTerm();
            else return value;
        }
    }

    private double parseTerm() {
        double value = parseUnary();
        while (true) {
            skipSpaces();
            if (eat('*')) value *= parseUnary();
            else if (eat('/')) value /= parseUnary();
            else if (eat('%')) value %= parseUnary();
            else return value;
        }
    }

    private double parseUnary() {
        skipSpaces();
        if (eat('-')) return -parseUnary();
        if (eat('+')) return parseUnary();
        return parsePower();
    }

    private double parsePower() {
        double base = parsePrimary();
        if (eat('^')) return Math.pow(base, parseUnary());   // 右结合,且指数可以是负数
        return base;
    }

    private double parsePrimary() {
        skipSpaces();
        if (eat('(')) {
            double value = parseExpression();
            if (!eat(')')) throw new IllegalArgumentException("缺少右括号");
            return value;
        }
        if (i < s.length() && (Character.isDigit(s.charAt(i)) || s.charAt(i) == '.')) {
            return parseNumber();
        }
        if (i < s.length() && Character.isLetter(s.charAt(i))) {
            String name = parseIdentifier().toLowerCase(Locale.ROOT);
            skipSpaces();
            if (i < s.length() && s.charAt(i) == '(') {          // 函数调用
                i++;
                List<Double> args = new ArrayList<>();
                skipSpaces();
                if (!eat(')')) {
                    args.add(parseExpression());
                    while (eat(',')) args.add(parseExpression());
                    if (!eat(')')) throw new IllegalArgumentException("函数 " + name + " 缺少右括号");
                }
                return callFunction(name, args);
            }
            Double constant = CONSTANTS.get(name);
            if (constant == null) throw new IllegalArgumentException("未知标识符:" + name);
            return constant;
        }
        throw new IllegalArgumentException("表达式里有无法识别的内容");
    }

    // ---------------------------------------------------------------- 词法

    private double parseNumber() {
        int start = i;
        while (i < s.length() && (Character.isDigit(s.charAt(i)) || s.charAt(i) == '.')) i++;
        if (i < s.length() && (s.charAt(i) == 'e' || s.charAt(i) == 'E')) {   // 科学计数法
            int save = i;
            i++;
            if (i < s.length() && (s.charAt(i) == '+' || s.charAt(i) == '-')) i++;
            if (i < s.length() && Character.isDigit(s.charAt(i))) {
                while (i < s.length() && Character.isDigit(s.charAt(i))) i++;
            } else {
                i = save;                                                    // 不是指数,退回(可能是常量 e)
            }
        }
        try {
            return Double.parseDouble(s.substring(start, i));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("非法数字:" + s.substring(start, i));
        }
    }

    private String parseIdentifier() {
        int start = i;
        while (i < s.length() && (Character.isLetterOrDigit(s.charAt(i)) || s.charAt(i) == '_')) i++;
        return s.substring(start, i);
    }

    private void skipSpaces() {
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
    }

    private boolean eat(char c) {
        skipSpaces();
        if (i < s.length() && s.charAt(i) == c) {
            i++;
            return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- 函数白名单

    private static double callFunction(String name, List<Double> a) {
        switch (name) {
            case "sqrt": return unary(name, a, Math::sqrt);
            case "abs":  return unary(name, a, Math::abs);
            case "sin":  return unary(name, a, Math::sin);
            case "cos":  return unary(name, a, Math::cos);
            case "tan":  return unary(name, a, Math::tan);
            case "asin": return unary(name, a, Math::asin);
            case "acos": return unary(name, a, Math::acos);
            case "atan": return unary(name, a, Math::atan);
            case "ln":   return unary(name, a, Math::log);
            case "log":  return unary(name, a, Math::log10);
            case "exp":  return unary(name, a, Math::exp);
            case "pow":
                require(name, a, 2);
                return Math.pow(a.get(0), a.get(1));
            case "min":
                require(name, a, 2);
                return Math.min(a.get(0), a.get(1));
            case "max":
                require(name, a, 2);
                return Math.max(a.get(0), a.get(1));
            default:
                throw new IllegalArgumentException("不支持的函数:" + name);
        }
    }

    private static double unary(String name, List<Double> a, java.util.function.DoubleUnaryOperator op) {
        require(name, a, 1);
        return op.applyAsDouble(a.get(0));
    }

    private static void require(String name, List<Double> a, int expected) {
        if (a.size() != expected) {
            throw new IllegalArgumentException(name + "() 需要 " + expected + " 个参数,实际 " + a.size() + " 个");
        }
    }
}
