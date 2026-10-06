package io.github.likeelysia.formalagent.extract;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.likeelysia.formalagent.chat.Message;
import io.github.likeelysia.formalagent.exception.AgentException;
import io.github.likeelysia.formalagent.llm.LlmClient;
import java.util.List;
import org.springframework.stereotype.Component;
import io.github.likeelysia.formalagent.llm.ChatOptions;

/**
 * 知识点提取器:把"一块文本"交给大模型,拿回这一块里的知识点列表。
 * 注意:只做一次性调用,不走 ChatService(不碰会话历史)。
 */
@Component
public class KnowledgeExtractor {

    /** 系统提示词:给模型看的"岗位说明书"。这是要反复调优的部分,所以单独抽出来放最上面。 */
    private static final String SYSTEM_PROMPT = """
                你是一个课程内容分析助手。用户会给你一段教材/讲义文本。
                请从中提取"知识点",并以 json 数组的形式输出;只输出 json,不要输出任何其他文字,不要用 Markdown 代码块。

                输出格式(json 数组):
                [
                  {"name": "知识点名称(短,不超过15字)", "detail": "一句话说明这个知识点是什么"},
                  ...
                ]

                要求:
                1. 只提炼这段文本里真正讲到的内容,不要自己补充文本以外的知识;
                2. 同一个知识点只出现一次;
                3. 如果这段文本里没有可提取的知识点(比如只是目录、页码),返回空数组 []。
                """;

    private final LlmClient client;
    private final ObjectMapper mapper;               // ← 容器注入的统一实例(不再是各自 new)

    public KnowledgeExtractor(LlmClient client, ObjectMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    /** 一块文本 → 这块里的知识点列表。空文本直接返回空列表(不浪费一次请求)。 */
    public List<KnowledgePoint> extract(String chunk) {
        if (chunk == null || chunk.isBlank()) return List.of();

        String raw = client.chat(
                List.of(new Message("system", SYSTEM_PROMPT), new Message("user", chunk)),
                ChatOptions.JSON);          // ← 关键:要求模型以 JSON 输出
        return parse(raw);
    }

    /** 把模型返回的文本"洗"成 JSON,再反序列化成对象列表。 */
    private List<KnowledgePoint> parse(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        String json = extractJsonArray(raw);
        try {
            List<KnowledgePoint> points = mapper.readValue(json, new TypeReference<List<KnowledgePoint>>() {});
            return List.copyOf(points);
        } catch (Exception e) {
            throw new AgentException("模型返回的不是预期的 JSON 数组:" + abbreviate(raw), e);
        }
    }

    /** 去掉 Markdown 代码围栏等噪音,并截取最外层的 [...]。 */
    private static String extractJsonArray(String raw) {
        String s = raw.strip();
        if (s.startsWith("```")) {
            int nl = s.indexOf('\n');
            if (nl >= 0) s = s.substring(nl + 1);
            int fence = s.lastIndexOf("```");
            if (fence >= 0) s = s.substring(0, fence);
            s = s.strip();
        }
        int start = s.indexOf('[');
        int end = s.lastIndexOf(']');
        if (start >= 0 && end > start) s = s.substring(start, end + 1);
        return s;
    }

    private String abbreviate(String s) {
        String one = s.replaceAll("\\s+", " ").strip();
        return one.length() <= 200 ? one : one.substring(0, 200) + "...";
    }
}