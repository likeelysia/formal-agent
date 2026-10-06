package io.github.likeelysia.formalagent.service;

import io.github.likeelysia.formalagent.chat.Message;
import io.github.likeelysia.formalagent.knowledge.KnowledgeItem;
import io.github.likeelysia.formalagent.knowledge.KnowledgeStore;
import io.github.likeelysia.formalagent.llm.LlmClient;
import io.github.likeelysia.formalagent.llm.VisionClient;
import java.nio.file.Path;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 图片问答:图片 →(识别)→(检索)→ 基于知识库生成"人话"回答 + 出处。 */
@Component
public class QaService {

    /** 第一步:让视觉模型用一句话概括图片讲什么(当检索的"查询词") */
    private static final String VISION_PROMPT =
            "用一句话概括这张图片讲的内容或提出的问题,只输出这一句,不要解释。";

    /** 第三步:生成回答的系统提示词——约束"只依据知识点 + 标出处" */
    private static final String ANSWER_SYSTEM_PROMPT = """
              你是一个教材讲解助手。请**只依据**下面提供的「知识点」,用通俗易懂的语言解释用户图片里的内容。
              规则:
              1. 只使用给出的知识点,不要编造知识点以外的内容;
              2. 如果知识点不足以回答,如实说明"教材里没有相关内容";
              3. 用大白话,面向初学者;
              4. 回答最后单独一行列出出处,格式:【出处】知识点名 —— 来源 位置。
              """;

    private final VisionClient vision;
    private final KnowledgeStore store;
    private final LlmClient llm;
    private final int topK;

    public QaService(VisionClient vision, KnowledgeStore store, LlmClient llm,
                     @Value("${fa.qa.top-k:5}") int topK) {
        this.vision = vision;
        this.store = store;
        this.llm = llm;
        this.topK = topK;
    }

    /** 一张图片 → 一段基于知识库的回答(带出处)。 */
    public String ask(Path image) {
        String query = vision.ask(image, VISION_PROMPT);              // ① 识别
        List<KnowledgeItem> hits = store.search(query, topK);          // ② 检索
        if (hits.isEmpty()) {
            return "教材里没有找到与该图片相关的内容(知识库为空或不匹配)。";
        }
        String answer = llm.chat(List.of(                                // ③ 生成
                new Message("system", ANSWER_SYSTEM_PROMPT),
                new Message("user", "图片内容:" + query + "\n\n可用知识点:\n" + format(hits))));
        return answer;
    }

    private static String format(List<KnowledgeItem> hits) {
        StringBuilder sb = new StringBuilder();
        for (KnowledgeItem k : hits) {
            sb.append("- ").append(k.name()).append(":").append(k.detail())
                    .append("(出处:").append(k.source())
                    .append(k.location() == null ? "" : " " + k.location()).append(")\n");
        }
        return sb.toString();
    }
}