package io.github.likeelysia.formalagent.web;

import io.github.likeelysia.formalagent.knowledge.KnowledgeItem;
import io.github.likeelysia.formalagent.knowledge.KnowledgeStore;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 知识库概况:GET /api/knowledge?sample=5 */
@RestController
@RequestMapping("/api")
public class KnowledgeController {

    private final KnowledgeStore store;

    public KnowledgeController(KnowledgeStore store) {
        this.store = store;
    }

    @GetMapping("/knowledge")
    public Map<String, Object> knowledge(@RequestParam(defaultValue = "5") int sample) {
        List<KnowledgeItem> all = store.all();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("count", all.size());
        body.put("sources", all.stream().map(KnowledgeItem::source).distinct().toList());
        body.put("sample", all.stream()
                .limit(Math.max(0, sample))
                .map(k -> k.name() + " / " + k.location())
                .toList());
        return body;
    }
}
