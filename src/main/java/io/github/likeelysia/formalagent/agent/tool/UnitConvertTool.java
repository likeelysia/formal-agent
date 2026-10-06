package io.github.likeelysia.formalagent.agent.tool;

import io.github.likeelysia.formalagent.agent.Tool;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/** 工具:单位换算(长度 / 质量 / 时间 / 角度 / 温度)。 */
@Component
public class UnitConvertTool implements Tool {

    /** 一个单位:属于哪一类 + 换算到该类"基准单位"的系数(温度特殊,系数无意义)。 */
    private record Unit(String group, double toBase) {
    }

    private static final Map<String, Unit> UNITS = new HashMap<>();

    private static void unit(String group, double toBase, String... names) {
        for (String n : names) {
            UNITS.put(n.toLowerCase(Locale.ROOT), new Unit(group, toBase));
        }
    }

    static {
        unit("长度", 1, "m", "米");
        unit("长度", 1000, "km", "千米", "公里");
        unit("长度", 0.01, "cm", "厘米");
        unit("长度", 0.001, "mm", "毫米");
        unit("长度", 1e-6, "um", "μm", "微米");
        unit("长度", 1e-9, "nm", "纳米");
        unit("长度", 1609.344, "mile", "mi", "英里");
        unit("长度", 0.3048, "ft", "foot", "英尺");
        unit("长度", 0.0254, "in", "inch", "英寸");
        unit("长度", 9.4607304725808e15, "ly", "光年");

        unit("质量", 1, "kg", "千克", "公斤");
        unit("质量", 1e-3, "g", "克");
        unit("质量", 1e-6, "mg", "毫克");
        unit("质量", 1000, "t", "吨");
        unit("质量", 0.45359237, "lb", "磅");
        unit("质量", 0.028349523125, "oz", "盎司");

        unit("时间", 1, "s", "秒");
        unit("时间", 1e-3, "ms", "毫秒");
        unit("时间", 60, "min", "分钟");
        unit("时间", 3600, "h", "小时");
        unit("时间", 86400, "day", "天");

        unit("角度", 1, "rad", "弧度");
        unit("角度", Math.PI / 180, "deg", "°", "度");

        unit("温度", Double.NaN, "k", "开尔文");
        unit("温度", Double.NaN, "c", "℃", "摄氏度");
        unit("温度", Double.NaN, "f", "℉", "华氏度");
    }

    @Override
    public String name() {
        return "unit_convert";
    }

    @Override
    public String description() {
        return "把数值从一个单位换算成另一个单位。支持长度(m/km/cm/mm/um/nm/mile/ft/in/ly)、"
                + "质量(kg/g/mg/t/lb/oz)、时间(s/ms/min/h/day)、角度(rad/deg)、温度(K/℃/℉);也认中文名。"
                + "当题目里需要单位换算时使用它。";
    }

    @Override
    public Map<String, Object> parameters() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "value", Map.of("type", "number", "description", "要换算的数值"),
                        "from", Map.of("type", "string", "description", "原单位,如 km"),
                        "to", Map.of("type", "string", "description", "目标单位,如 m")),
                "required", List.of("value", "from", "to"));
    }

    @Override
    public String execute(Map<String, Object> args) {
        double value = Tool.doubleArg(args, "value");
        String fromRaw = Tool.strArg(args, "from");
        String toRaw = Tool.strArg(args, "to");
        String from = normalize(fromRaw);
        String to = normalize(toRaw);

        Unit source = UNITS.get(from);
        Unit target = UNITS.get(to);
        if (source == null) return "不认识这个单位:" + fromRaw;
        if (target == null) return "不认识这个单位:" + toRaw;
        if (!source.group().equals(target.group())) {
            return "单位类别不同(" + source.group() + " vs " + target.group() + "),不能直接换算。";
        }

        double result = "温度".equals(source.group())
                ? convertTemperature(value, from, to)
                : value * source.toBase() / target.toBase();
        return Tool.num(value) + " " + fromRaw + " = " + Tool.num(result) + " " + toRaw;
    }

    private static String normalize(String s) {
        return s == null ? "" : s.strip().toLowerCase(Locale.ROOT);
    }

    /** 温度:先折成开尔文,再换成目标(摄氏/华氏带偏移,不能简单乘系数)。 */
    private static double convertTemperature(double v, String from, String to) {
        double kelvin = switch (from) {
            case "c", "℃", "摄氏度" -> v + 273.15;
            case "f", "℉", "华氏度" -> (v - 32) * 5.0 / 9.0 + 273.15;
            default -> v;                                  // 已经是开尔文
        };
        return switch (to) {
            case "c", "℃", "摄氏度" -> kelvin - 273.15;
            case "f", "℉", "华氏度" -> (kelvin - 273.15) * 9.0 / 5.0 + 32;
            default -> kelvin;
        };
    }
}
