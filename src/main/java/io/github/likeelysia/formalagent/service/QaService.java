package io.github.likeelysia.formalagent.service;

import io.github.likeelysia.formalagent.chat.Message;
import io.github.likeelysia.formalagent.knowledge.KnowledgeItem;
import io.github.likeelysia.formalagent.knowledge.KnowledgeStore;
import io.github.likeelysia.formalagent.llm.LlmClient;
import io.github.likeelysia.formalagent.llm.VisionClient;
import io.github.likeelysia.formalagent.prompt.Prompts;
import java.nio.file.Path;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 图片问答:图片 →(识别)→(检索)→ 基于知识库生成"人话"回答 + 出处。 */
@Component
public class QaService {

    // 提示词已外置:resources/prompts/image-question.txt 与 answer-system.txt

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
        String query = vision.ask(image, Prompts.get("image-question"));   // ① 识别
        return answer(query, "图片内容");
    }

    /** 一个文本问题 → 一段基于知识库的回答(带出处)。 */
    public String askText(String question) {
        return answer(question, "用户问题");
    }

    /** 公共流程:检索 → 生成。 */
    private String answer(String query, String label) {
        List<KnowledgeItem> hits = store.search(query, topK);          // ② 检索(向量)
        if (hits.isEmpty()) {
            return "教材里没有找到与此相关的内容(知识库为空或不匹配)。";
        }
        return llm.chat(List.of(                                       // ③ 生成
                new Message("system", Prompts.get("answer-system")),
                new Message("user", label + ":" + query + "\n\n可用知识点:\n" + format(hits))));
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