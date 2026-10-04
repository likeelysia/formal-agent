package io.github.likeelysia.formalagent.llm;

/**
 * 一次模型调用的可选参数。
 *
 * <p>目前只有一项:是否要求"结构化输出"(JSON mode)。
 * 做成对象而不是加 boolean 参数,是为了以后扩展(temperature、stream、tools…)
 * 时不用改方法签名 —— 新增字段即可,调用点不受影响。
 */
public record ChatOptions(boolean jsonMode) {

    /** 默认:普通对话 */
    public static final ChatOptions DEFAULT = new ChatOptions(false);

    /** 要求模型以合法 JSON 输出(DeepSeek/OpenAI 的 response_format=json_object) */
    public static final ChatOptions JSON = new ChatOptions(true);
}