package io.github.likeelysia.formalagent.agent.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 单位换算工具测试。 */
class UnitConvertToolTest {

    private final UnitConvertTool tool = new UnitConvertTool();

    private String convert(Object value, String from, String to) {
        return tool.execute(Map.of("value", value, "from", from, "to", to));
    }

    @Test
    @DisplayName("长度:km → m / mile → m")
    void length() {
        assertEquals("1 km = 1000 m", convert(1, "km", "m"));
        assertEquals("1 mile = 1609.34 m", convert(1, "mile", "m"));
    }

    @Test
    @DisplayName("质量 / 时间")
    void massAndTime() {
        assertEquals("2 kg = 2000 g", convert(2, "kg", "g"));
        assertEquals("1 h = 3600 s", convert(1, "h", "s"));
    }

    @Test
    @DisplayName("温度带偏移:100℃ = 212℉;-40 是两者的交点")
    void temperature() {
        assertEquals("100 ℃ = 212 ℉", convert(100, "℃", "℉"));
        assertEquals("-40 ℃ = -40 ℉", convert(-40, "℃", "℉"));
        assertEquals("0 ℃ = 273.15 K", convert(0, "℃", "K"));
    }

    @Test
    @DisplayName("中文单位名也认")
    void chineseNames() {
        assertEquals("1 千米 = 1000 米", convert(1, "千米", "米"));
    }

    @Test
    @DisplayName("类别不同 / 不认识 → 给提示,不崩")
    void friendlyErrors() {
        assertTrue(convert(1, "kg", "m").contains("类别不同"));
        assertTrue(convert(1, "xxx", "m").contains("不认识"));
        assertTrue(convert(1, "m", "yyy").contains("不认识"));
    }
}
