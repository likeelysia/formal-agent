package io.github.likeelysia.formalagent.agent.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 计算器工具测试:既验算术,也验"安全性"(注入类输入必须被拒)。 */
class CalculatorToolTest {

    private final CalculatorTool tool = new CalculatorTool();

    private String calc(String expression) {
        return tool.execute(Map.of("expression", expression));
    }

    @Test
    @DisplayName("四则运算与优先级、乘方")
    void arithmetic() {
        assertEquals("1+2*3 = 7", calc("1+2*3"));
        assertEquals("(1+2)*3 = 9", calc("(1+2)*3"));
        assertEquals("10%3 = 1", calc("10%3"));
        assertEquals("2^10 = 1024", calc("2^10"));
        assertEquals("-2^2 = -4", calc("-2^2"));
        assertEquals("3e8*2 = 600000000", calc("3e8*2"));
    }

    @Test
    @DisplayName("白名单函数与常量")
    void functionsAndConstants() {
        assertEquals("sqrt(2) = 1.41421", calc("sqrt(2)"));
        assertEquals("pi = 3.14159", calc("pi"));
        assertTrue(calc("sin(pi/2)").endsWith("= 1"));
        assertEquals("pow(2,10) = 1024", calc("pow(2,10)"));
    }

    @Test
    @DisplayName("空表达式给提示,不抛")
    void emptyExpression() {
        assertEquals("expression 不能为空", calc("  "));
    }

    @Test
    @DisplayName("安全:不认变量、不认方法调用、不认未知函数、参数个数不对")
    void rejectsUnsafeInput() {
        assertThrows(IllegalArgumentException.class, () -> calc("x+1"));
        assertThrows(IllegalArgumentException.class, () -> calc("os.system(\"rm -rf /\")"));
        assertThrows(IllegalArgumentException.class, () -> calc("Runtime.getRuntime()"));
        assertThrows(IllegalArgumentException.class, () -> calc("1+*2"));
        assertThrows(IllegalArgumentException.class, () -> calc("sqrt(1,2)"));
        assertThrows(IllegalArgumentException.class, () -> calc("(1+2"));
    }
}
