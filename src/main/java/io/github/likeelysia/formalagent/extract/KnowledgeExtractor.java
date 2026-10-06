package io.github.likeelysia.formalagent.extract;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.likeelysia.formalagent.chat.Message;
import io.github.likeelysia.formalagent.exception.AgentException;
import io.github.likeelysia.formalagent.llm.LlmClient;
import io.github.likeelysia.formalagent.prompt.Prompts;
import java.util.List;
import org.springframework.stereotype.Component;
import io.github.likeelysia.formalagent.llm.ChatOptions;

/**
 * 知识点提取器:把"一块文本"交给大模型,拿回这一块里的知识点列表。
 * 注意:只做一次性调用,不走 ChatService(不碰会话历史)。
 */
@Component
public class KnowledgeExtractor {

    // 系统提示词已外置到 resources/prompts/knowledge-extract.txt

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
                List.of(new Message("system", Prompts.get("knowledge-extract")), new Message("user", chunk)),
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