package io.github.likeelysia.formalagent.agent.tool;

import io.github.likeelysia.formalagent.agent.Tool;
import io.github.likeelysia.formalagent.knowledge.KnowledgeItem;
import io.github.likeelysia.formalagent.knowledge.KnowledgeStore;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** 工具三:按"来源 + 页码"精确取知识点。补向量检索的"模糊"—— 要精确位置时用它。 */
@Component
public class KnowledgePageTool implements Tool {

    private final KnowledgeStore store;

    public KnowledgePageTool(KnowledgeStore store) {
        this.store = store;
    }

    @Override
    public String name() {
        return "get_knowledge_page";
    }

    @Override
    public String description() {
        return "按“教材名 + 页码”精确取出那一页的知识点(向量检索是模糊找,这个是精确定位)。"
                + "当用户明确提到某本教材的某一页时使用。";
    }

    @Override
    public Map<String, Object> parameters() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "source", Map.of("type", "string", "description", "教材名(文件名的部分匹配即可)"),
                        "page", Map.of("type", "integer", "description", "页码(从 1 开始)")),
                "required", List.of("source", "page"));
    }

    @Override
    public String execute(Map<String, Object> args) {
        String source = Tool.strArg(args, "source");
        int page = Tool.intArg(args, "page", -1);
        if (source.isBlank() || page <= 0) {
            return "source 和 page 都必须给出(page 从 1 开始)";
        }
        String location = "第 " + page + " 页";
        List<KnowledgeItem> hits = store.all().stream()
                .filter(k -> k.source() != null && k.source().contains(source))
                .filter(k -> location.equals(k.location()))
                .toList();
        if (hits.isEmpty()) {
            return "没有找到 \"" + source + "\" 的" + location + " 的内容。";
        }
        return Tool.format(hits);
    }
}
