package io.github.likeelysia.formalagent.knowledge;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.likeelysia.formalagent.exception.AgentException;
import io.github.likeelysia.formalagent.llm.EmbeddingClient;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * JSON 文件实现:整个知识库就是一份 JSON 数组;启动读、写入存。
 *
 * <p><b>检索</b>:优先"向量检索" —— 把问题也算成向量,取余弦相似度最高的 topK 条;
 * embedding 服务不可用时<b>降级</b>成原来的关键词匹配(不会因为服务没开就搜不了)。
 *
 * <p>向量存放在 sidecar 文件({@code base.vectors.json}),避免每次启动重算;文件里带
 * "模型指纹",模型一换旧向量作废、自动重算。
 */
@Component
public class JsonKnowledgeStore implements KnowledgeStore {

    /** 向量 sidecar 的内容。 */
    record VectorFile(String model, int dim, Map<String, float[]> vectors) {
    }

    /** 排序用的临时结构:知识点 + 得分。 */
    private record Scored(KnowledgeItem item, double score) {
    }

    private final ObjectMapper mapper;
    private final EmbeddingClient embedder;
    private final Path file;
    private final Path vectorFile;
    private final Map<String, KnowledgeItem> items = new LinkedHashMap<>();   // id → item(保序 + 去重)
    private final Map<String, float[]> vectors = new LinkedHashMap<>();       // id → 向量

    public JsonKnowledgeStore(ObjectMapper mapper, EmbeddingClient embedder,
                              @Value("${knowledge.file:knowledge/base.json}") String filePath) {
        this.mapper = mapper;
        this.embedder = embedder;
        this.file = Paths.get(filePath);
        // base.json → base.vectors.json(向量另存一份,别把知识库本体撑爆)
        this.vectorFile = Paths.get(filePath.replaceAll("\\.json$", "") + ".vectors.json");
        load();
    }

    // ---------------------------------------------------------------- 读 / 写

    private void load() {
        if (Files.exists(file)) {
            try {
                List<KnowledgeItem> loaded = mapper.readValue(
                        Files.readString(file, StandardCharsets.UTF_8),
                        new TypeReference<List<KnowledgeItem>>() {});
                for (KnowledgeItem item : loaded) items.put(item.id(), item);
            } catch (IOException e) {
                throw new AgentException("读取知识库失败:" + file, e);
            }
        }
        loadVectors();
    }

    private void loadVectors() {
        if (!Files.exists(vectorFile)) return;
        try {
            VectorFile vf = mapper.readValue(
                    Files.readString(vectorFile, StandardCharsets.UTF_8), VectorFile.class);
            if (!embedder.modelName().equals(vf.model())) {      // 模型换了 → 旧向量作废
                System.out.println("[知识库] embedding 模型变了(" + vf.model() + " → "
                        + embedder.modelName() + "),旧向量作废,下次检索时重算");
                return;
            }
            vf.vectors().forEach((id, v) -> {
                if (items.containsKey(id)) vectors.put(id, v);
            });
        } catch (IOException e) {
            System.out.println("[知识库] 向量文件读取失败,将重算:" + e.getMessage());
        }
    }

    @Override
    public void addAll(List<KnowledgeItem> newItems) {
        for (KnowledgeItem item : newItems) items.put(item.id(), item);   // 同 id 覆盖
        persist();
        ensureVectors();                                                  // 顺手把新条目的向量算好
    }

    @Override
    public List<KnowledgeItem> all() {
        return List.copyOf(items.values());
    }

    // ---------------------------------------------------------------- 检索

    @Override
    public List<KnowledgeItem> search(String query, int topK) {
        if (query == null || query.isBlank() || items.isEmpty()) return List.of();

        if (vectors.size() < items.size()) ensureVectors();     // 自愈:服务恢复后自动补齐

        float[] queryVector = tryEmbed(query);
        if (queryVector == null) {
            return keywordSearch(query, topK);                  // 降级:向量服务不可用
        }

        List<Scored> scored = new ArrayList<>();
        for (KnowledgeItem item : items.values()) {
            float[] v = vectors.get(item.id());
            if (v != null) scored.add(new Scored(item, cosine(queryVector, v)));
        }
        if (scored.isEmpty()) return keywordSearch(query, topK);

        scored.sort(Comparator.comparingDouble(Scored::score).reversed());
        return scored.stream()
                .limit(Math.max(0, topK))
                .map(Scored::item)
                .toList();
    }

    /** 把条目向量补齐;失败即止(说明服务没开),不逐条傻等。 */
    private void ensureVectors() {
        List<KnowledgeItem> missing = items.values().stream()
                .filter(k -> vectors.get(k.id()) == null)
                .toList();
        if (missing.isEmpty()) return;

        List<String> texts = missing.stream()
                .map(k -> k.name() + " " + (k.detail() == null ? "" : k.detail()))
                .toList();
        try {
            List<float[]> computed = embedder.embedAll(texts);
            for (int i = 0; i < missing.size(); i++) {
                vectors.put(missing.get(i).id(), computed.get(i));
            }
            persistVectors();
        } catch (RuntimeException e) {
            System.out.println("[知识库] embedding 服务不可用,暂用关键词检索:" + e.getMessage());
        }
    }

    private float[] tryEmbed(String text) {
        try {
            return embedder.embed(text);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** 余弦相似度:-1~1,越接近 1 越像。 */
    private static double cosine(float[] a, float[] b) {
        double dot = 0, na = 0, nb = 0;
        int n = Math.min(a.length, b.length);
        for (int i = 0; i < n; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0 || nb == 0) return 0;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    // ---------------------------------------------------------------- 降级:关键词

    /** 老办法:按空白/标点切词,命中越多分越高(中文无空格时退化为整串子串匹配)。 */
    private List<KnowledgeItem> keywordSearch(String query, int topK) {
        String[] terms = query.toLowerCase(Locale.ROOT)
                .split("[\\s,，。、;；:：!?！?()（）\\[\\]]+");
        List<KnowledgeItem> scored = new ArrayList<>();
        List<Integer> scores = new ArrayList<>();
        for (KnowledgeItem item : items.values()) {
            String haystack = (item.name() + " " + item.detail()).toLowerCase(Locale.ROOT);
            int score = 0;
            for (String t : terms) if (!t.isBlank() && haystack.contains(t)) score++;
            if (score > 0) { scored.add(item); scores.add(score); }
        }
        for (int i = 0; i < scored.size(); i++) {
            for (int j = i + 1; j < scored.size(); j++) {
                if (scores.get(j) > scores.get(i)) {
                    var ti = scored.set(i, scored.get(j)); scored.set(j, ti);
                    var si = scores.set(i, scores.get(j)); scores.set(j, si);
                }
            }
        }
        return List.copyOf(scored.subList(0, Math.min(Math.max(0, topK), scored.size())));
    }

    // ---------------------------------------------------------------- 落盘

    private void persist() {
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) Files.createDirectories(parent);
            Files.writeString(file,
                    mapper.writerWithDefaultPrettyPrinter().writeValueAsString(List.copyOf(items.values())),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AgentException("写入知识库失败:" + file, e);
        }
    }

    /** 向量落盘失败不该影响本次运行(顶多下次重算)。 */
    private void persistVectors() {
        try {
            Path parent = vectorFile.toAbsolutePath().getParent();
            if (parent != null) Files.createDirectories(parent);
            int dim = vectors.values().stream().findFirst().map(v -> v.length).orElse(0);
            Files.writeString(vectorFile,
                    mapper.writeValueAsString(new VectorFile(embedder.modelName(), dim, vectors)),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.out.println("[知识库] 向量落盘失败(不影响本次运行):" + e.getMessage());
        }
    }
}
