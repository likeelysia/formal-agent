package io.github.likeelysia.formalagent.extract;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.likeelysia.formalagent.doc.DocPipeline;
import io.github.likeelysia.formalagent.doc.PlainTextReader;
import io.github.likeelysia.formalagent.doc.TextSplitter;
import io.github.likeelysia.formalagent.knowledge.JsonKnowledgeStore;
import io.github.likeelysia.formalagent.knowledge.KnowledgeItem;
import io.github.likeelysia.formalagent.llm.LlmClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import io.github.likeelysia.formalagent.doc.PdfReader;
import io.github.likeelysia.formalagent.doc.ReaderRegistry;
import io.github.likeelysia.formalagent.doc.ImageReader;

class IngestServiceTest {

    @TempDir Path tmp;
    private final ObjectMapper mapper = new ObjectMapper();
    private JsonKnowledgeStore store;
    private IngestService service;
    private Path file;

    @BeforeEach
    void setUp() throws Exception {
        file = tmp.resolve("book.md");
        Files.writeString(file, "aaaa\n\nbbbb\n\ncccc", StandardCharsets.UTF_8);

        DocPipeline pipeline = new DocPipeline(
                new ReaderRegistry(new PlainTextReader(), new PdfReader((image, prompt) -> ""),
                        new ImageReader((image, prompt) -> "")),
                new TextSplitter(20, 5));
        LlmClient fake = (history, options) -> "[{\"name\":\"K1\",\"detail\":\"d1\"}]";
        KnowledgeExtractor extractor = new KnowledgeExtractor(fake, mapper);
        store = new JsonKnowledgeStore(mapper, text -> new float[]{1f, 0f},
                tmp.resolve("base.json").toString(), 0);
        service = new IngestService(pipeline, extractor, store);
    }

    @Test
    @DisplayName("教材 → 知识点入库:同名去重,记录出处")
    void ingestsIntoStore() {
        List<KnowledgeItem> items = service.ingest(file);

        assertEquals(1, items.size(), "三块都返回 K1 → 去重后 1 条");
        assertEquals("book.md", items.get(0).source(), "应记录来源文件");
        assertEquals(1, store.all().size(), "知识库里应有 1 条");
    }
}