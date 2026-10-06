package io.github.likeelysia.formalagent.agent.tool;

import io.github.likeelysia.formalagent.agent.Tool;
import io.github.likeelysia.formalagent.knowledge.KnowledgeStore;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** 工具一:按语义检索知识库(把现有的向量检索包成"模型可调用的能力")。 */
@Component
public class SearchKnowledgeTool implements Tool {

    private final KnowledgeStore store;

    public SearchKnowledgeTool(KnowledgeStore store) {
        this.store = store;
    }

    @Override
    public String name() {
        return "search_knowledge";
    }

    @Override
    public String description() {
        return "从教材知识库里按语义检索相关知识点(返回内容 + 出处)。"
                + "当用户问教材里的具体内容时调用它;可以用不同关键词多调几次来补充信息。";
    }

    @Override
    public Map<String, Object> parameters() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "query", Map.of("type", "string", "description", "检索用的关键词或问题"),
                        "top_k", Map.of("type", "integer", "description", "返回条数,默认 5")),
                "required", List.of("query"));
    }

    @Override
    public String execute(Map<String, Object> args) {
        String query = Tool.strArg(args, "query");
        if (query.isBlank()) return "query 不能为空";
        int topK = Tool.intArg(args, "top_k", 5);
        return Tool.format(store.search(query, topK));
    }
}
