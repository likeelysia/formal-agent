package io.github.likeelysia.formalagent.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * 应用配置:集中管理 config.properties 里的配置项。
 * 好处:改配置不用改代码(改完直接重启程序即可生效)。
 */
public final class AppConfig {                        // ① final:这个类不希望被继承

    /** 配置文件在 classpath 里的位置。"开头那个 / "表示从 classpath 根开始找 */
    private static final String CONFIG_FILE = "/config.properties";

    /** 一个程序只需要一份配置 → 用 static final 持有,类加载时读一次就够 */
    private static final Properties PROPS = new Properties();

    // ② 静态代码块:类第一次被用到时执行【一次】,负责把配置读进来
    static {
        try (InputStream in = AppConfig.class.getResourceAsStream(CONFIG_FILE)) {
            // 找不到会返回 null(而不是抛异常)→ 必须自己判空,否则后面 load 会空指针
            if (in == null) {
                throw new IllegalStateException("找不到配置文件:" + CONFIG_FILE
                        + "(确认它在 src/main/resources 下,并且该目录被标记为资源根)");
            }
            // ③ 用转换流指定 UTF-8:Properties 默认按 ISO-8859-1 解码,中文会乱码
            PROPS.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            // ④ 配置读不到 = 程序不该继续跑 → 立刻失败(fail fast),包成运行时异常往外抛
            throw new IllegalStateException("读取配置文件失败:" + CONFIG_FILE, e);
        }
    }

    /** ⑤ 私有构造器:工具类,不允许别人 new(全是静态方法,new 出来没意义) */
    private AppConfig() {
    }

    // ================= 对外只暴露"语义化"的取值方法 =================

    public static String apiUrl() {
        return PROPS.getProperty("deepseek.api.url");
    }

    public static String model() {
        return PROPS.getProperty("deepseek.model");
    }

    /** ⑥ 带默认值 + 类型转换:配置里没有这条也能跑 */
    public static int maxTokens() {
        return Integer.parseInt(PROPS.getProperty("deepseek.maxTokens", "200"));
    }

    public static int timeoutSeconds() {
        return Integer.parseInt(PROPS.getProperty("deepseek.timeoutSeconds", "30"));
    }
    /** 调试开关:true 时才打印原始 JSON 等调试信息 */
    public static boolean debug() {
        return Boolean.parseBoolean(PROPS.getProperty("app.debug", "false"));
    }
}