package io.github.likeelysia.formalagent.web;

import io.github.likeelysia.formalagent.agent.AgentResult;
import io.github.likeelysia.formalagent.agent.AgentService;
import io.github.likeelysia.formalagent.exception.AgentException;
import io.github.likeelysia.formalagent.llm.VisionClient;
import io.github.likeelysia.formalagent.prompt.Prompts;
import io.github.likeelysia.formalagent.service.FileStorage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Agent 问答接口:POST /api/agent
 *
 * <p>支持两种调用:
 * <ul>
 *   <li>{@code application/json} —— {@code {"question":"..."}} 纯文本提问;</li>
 *   <li>{@code multipart/form-data} —— 上传 <b>图片</b>(可选再带 {@code question});
 *       先让视觉模型"看懂"图片,再把图片内容交给 Agent 去查知识库回答。</li>
 * </ul>
 *
 * <p>和以前的"固定流水线"不同:这里把"工具"交给模型,由它自己决定查不查、查几次。
 * 返回里带上 {@code trace}(它调了哪些工具),方便观察它到底"想了什么"。
 */
@RestController
@RequestMapping("/api")
public class AgentController {

    private final AgentService agentService;
    private final VisionClient vision;
    private final FileStorage fileStorage;

    public AgentController(AgentService agentService, VisionClient vision, FileStorage fileStorage) {
        this.agentService = agentService;
        this.vision = vision;
        this.fileStorage = fileStorage;
    }

    /** 请求体:{"question":"沿闭合曲线跑一圈回到原点,位移是多少?"} */
    public record AgentRequest(String question) {
    }

    /** 响应体:{"answer":"...【出处】...","steps":2,"trace":["search_knowledge({...})"]} */
    public record AgentResponse(String answer, int steps, List<String> trace) {
    }

    /** 纯文本提问。 */
    @PostMapping(value = "/agent", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AgentResponse agent(@RequestBody AgentRequest req) {
        if (req == null || req.question() == null || req.question().isBlank()) {
            throw new AgentException("question 不能为空");
        }
        return toResponse(agentService.run(req.question()));
    }

    /** 图片提问:上传图片 →(视觉识别)→ 交给 Agent 回答。 */
    @PostMapping(value = "/agent", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AgentResponse agentWithImage(@RequestParam("file") MultipartFile file,
                                        @RequestParam(value = "question", required = false) String question) {
        if (file == null || file.isEmpty()) {
            throw new AgentException("file 不能为空");
        }
        Path stored;
        try {
            stored = fileStorage.store(file.getOriginalFilename(), file.getInputStream());
        } catch (IOException e) {
            throw new AgentException("读取上传内容失败:" + e.getMessage(), e);
        }

        String described = vision.ask(stored, Prompts.get("image-question"));
        String finalQuestion = (question == null || question.isBlank())
                ? described
                : question + "\n(图片内容:" + described + ")";
        return toResponse(agentService.run(finalQuestion));
    }

    private static AgentResponse toResponse(AgentResult result) {
        return new AgentResponse(result.answer(), result.steps(), result.trace());
    }
}
