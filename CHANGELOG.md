# Changelog

本项目的所有重要变更都会记录在此文件。
格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/),版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

## [Unreleased]

### 新增 / Added
- **知识库**:`KnowledgeItem` / `KnowledgeStore`(接口) / `JsonKnowledgeStore`(JSON 落盘,换实现不动业务)
- **入库流水线**:`IngestService` + `IngestMain` —— 文档 → 知识点(带页码出处)→ 知识库
- **文档读取抽象化**:`DocumentReader.read` 返回带位置的文本段(`TextSegment`);`ReaderRegistry` 按扩展名分派
- **PDF 读取**:`PdfReader` —— ① 文字层(`PDFTextStripper`);② 无文字层(扫描件)→ 逐页渲染 + 视觉 OCR
- **图片 OCR / 图片问答**:`VisionClient` / `MoonshotVisionClient`(Kimi 视觉)、`ImageReader`、`QaService`(识图 → 检索 → 带出处回答)
- **语义检索(真 RAG 第一步)**:`EmbeddingClient`(接口)/ `LocalEmbeddingClient`(本地 llama.cpp);
  `JsonKnowledgeStore` 检索升级为**余弦相似度 top-K**,向量存 sidecar `knowledge/base.vectors.json`;
  embedding 服务不可用时**自动降级**为关键词匹配
- **Spring Boot 化**:父工程 `spring-boot-starter-parent:3.5.16`、`FormalAgentApplication`、
  `spring-boot-maven-plugin`(**可执行 fat jar**)、`@ConfigurationProperties`、`application.yml` + `dev` profile
- **健壮性**:Spring Retry 声明式**重试 + 退避**;单块提炼 / 单页 OCR 失败不再拖垮整批
- **命令行**:`ChatCli`(`CommandLineRunner`,由 Boot 调起)取代原 `Main`
- 本地 embedding 一键脚本 `start-embedding.bat`
- 单元测试 **54 → 74**(新增向量检索、图片读取、PDF 双策略、配置绑定等)

### 变更 / Changed
- **配置**:`config.properties` → **`application.yml`**(前缀 `fa.*`);`@Value` → **`@ConfigurationProperties`**
  (`AppConfig` 由散字段类改为 **record + 分组**),支持 profile 与环境变量覆盖
- **入口**:手动 `new AnnotationConfigApplicationContext(...)` → Boot `SpringApplication.run(...)`
- **视觉请求关闭"思考模式"**(`thinking.type = "disabled"`):OCR 场景更快(约 51s → 16s)且输出更稳
- 超时:LLM 请求读取超时提升到 **120s**(视觉 OCR 单页耗时较长);连接超时独立为 `fa.llm.connect-timeout-seconds`

### 修复 / Fixed
- **Kimi 视觉 `content` 偶发为空**(思考模式下答案全跑进 `reasoning_content`)→ 关闭思考 + 空内容按瞬时故障重试
- **429 限流**(组织并发上限=1)→ 逐页停顿 + 确定性退避(随机退避可能退到 0ms,对限流无效)
- 扫描版 PDF 无文字层导致"没有可提取内容" → 自动回退视觉 OCR
- 非 2xx 响应与缺少 `choices` 时给出明确错误(此前可能解析为空 / NPE)

## [0.1.0] - 2026-09-17

### 新增 / Added
- 初始化 Maven 项目骨架(`groupId: io.github.likeelysia`,`artifactId: formal-agent`)
- 配置 JUnit 5 测试依赖与 `maven-compiler-plugin` / `maven-surefire-plugin`
- 添加 MIT 许可证、README、`.gitignore`(含密钥防护规则)
- 接入 DeepSeek `/chat/completions`(JDK `HttpClient` + Jackson 解析)
- 多轮对话(`ChatSession` / `Message`)、会话存档(`SessionStore`)
- 文档 → 知识点提取流水线(`DocPipeline` → `KnowledgeExtractor` → `KnowledgeReport`)
- 结构化输出(JSON mode)、统一 `ObjectMapper` 单例与强类型 DTO
- 应用配置外置 + Spring IoC 装配 + 按天调用日志(`ApiLogger`)
