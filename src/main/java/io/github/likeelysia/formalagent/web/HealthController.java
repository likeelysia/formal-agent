package io.github.likeelysia.formalagent.web;

import io.github.likeelysia.formalagent.knowledge.KnowledgeStore;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 健康检查:GET /api/health —— 给运维/监控用,一眼看服务活着没、知识库有多少条。 */
@RestController
@RequestMapping("/api")
public class HealthController {

    private final KnowledgeStore store;

    public HealthController(KnowledgeStore store) {
        this.store = store;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("knowledgeCount", store.all().size());
        return body;
    }
}
