package io.github.likeelysia.formalagent.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.likeelysia.formalagent.chat.Message;
import io.github.likeelysia.formalagent.knowledge.JsonKnowledgeStore;
import io.github.likeelysia.formalagent.knowledge.KnowledgeItem;
import io.github.likeelysia.formalagent.llm.ChatOptions;
import io.github.likeelysia.formalagent.llm.LlmClient;
import io.github.likeelysia.formalagent.llm.VisionClient;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class QaServiceTest {

    @TempDir Path tmp;
    private final ObjectMapper mapper = new ObjectMapper();

    /** 假 LLM:固定答一句,并记住收到的 messages(好断言提示词内容) */
    private static class CapturingLlm implements LlmClient {
        private final String answer;
        private List<Message> lastHistory;
        CapturingLlm(String answer) { this.answer = answer; }
        @Override public String chat(List<Message> history, ChatOptions options) {
            this.lastHistory = history;
            return answer;
        }
    }

    @Test
    @DisplayName("图片 → 检索到知识点 → 提示词带上知识点和出处 → 返回回答")
    void answersWithKnowledge() {
        JsonKnowledgeStore store = new JsonKnowledgeStore(mapper, text -> new float[]{1f, 0f},
                tmp.resolve("kb.json").toString());
        store.addAll(List.of(new KnowledgeItem("1", "多态", "同一接口不同实现", "java.pdf", "第 12 页", "pdf")));

        VisionClient vision = (image, prompt) -> "多态";
        CapturingLlm llm = new CapturingLlm("多态就是:同一接口不同实现。【出处】多态 —— java.pdf 第 12 页");

        QaService qa = new QaService(vision, store, llm, 5);
        String answer = qa.ask(Path.of("some.png"));        // 假 client 不真读文件

        assertEquals("多态就是:同一接口不同实现。【出处】多态 —— java.pdf 第 12 页", answer);

        String prompt = llm.lastHistory.get(1).getContent();
        assertTrue(prompt.contains("多态"), "提示词里应带上检索到的知识点");
        assertTrue(prompt.contains("java.pdf"), "提示词里应带上出处");
        assertTrue(prompt.contains("第 12 页"), "提示词里应带上位置");
    }

    @Test
    @DisplayName("知识库没有相关内容 → 如实说明,不硬编")
    void noKnowledge() {
        JsonKnowledgeStore store = new JsonKnowledgeStore(mapper, text -> new float[]{1f, 0f},
                tmp.resolve("kb2.json").toString());
        VisionClient vision = (image, prompt) -> "量子力学";

        QaService qa = new QaService(vision, store, new CapturingLlm("x"), 5);

        assertTrue(qa.ask(Path.of("q.png")).contains("没有找到"));
    }
}