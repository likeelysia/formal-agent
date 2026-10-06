package io.github.likeelysia.formalagent.web;

import io.github.likeelysia.formalagent.exception.AgentException;
import io.github.likeelysia.formalagent.extract.IngestService;
import io.github.likeelysia.formalagent.knowledge.KnowledgeItem;
import io.github.likeelysia.formalagent.knowledge.KnowledgeStore;
import io.github.likeelysia.formalagent.service.FileStorage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 入库接口(管理端):把一份教材灌进知识库。
 *
 * <p><b>安全设计</b>:接口只收 <b>上传的文件内容</b>,不收"路径" ——
 * 客户端无法指定服务端读哪个文件,"任意文件读取 / 路径遍历"从根上被堵死。
 * 存哪、叫什么名字,全部由 {@link FileStorage} 决定(清洗文件名 + 白名单 + 固定目录)。
 *
 * <p>⚠️ 仍然待办:<b>鉴权</b>(现在谁都能调这个管理接口)。见 README 路线图。
 */
@RestController
@RequestMapping("/api")
public class IngestController {

    private final IngestService ingestService;
    private final KnowledgeStore store;
    private final FileStorage fileStorage;

    public IngestController(IngestService ingestService, KnowledgeStore store, FileStorage fileStorage) {
        this.ingestService = ingestService;
        this.store = store;
        this.fileStorage = fileStorage;
    }

    /** 响应体:{"added":73,"total":354,"storedAs":"20261006173000-物理.pdf"} */
    public record IngestResponse(int added, int total, String storedAs) {
    }

    @PostMapping(value = "/ingest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public IngestResponse ingest(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AgentException("file 不能为空");
        }
        Path stored;
        try {
            stored = fileStorage.store(file.getOriginalFilename(), file.getInputStream());
        } catch (IOException e) {
            throw new AgentException("读取上传内容失败:" + e.getMessage(), e);
        }
        List<KnowledgeItem> added = ingestService.ingest(stored);
        return new IngestResponse(added.size(), store.all().size(), stored.getFileName().toString());
    }
}
