# Changelog

本项目的所有重要变更都会记录在此文件。
格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/),版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

## [Unreleased]

### 新增 / Added
- 接入 DeepSeek `/chat/completions`(JDK `HttpClient` + Jackson 解析)
- 多轮对话:`ChatSession` / `Message`,保留历史上下文
- 会话存档:`SessionStore`(Java 原生序列化 `.bin` + 可读 JSON 导出 `.json`)
- 文档读取与切块:`DocumentReader` / `PlainTextReader` / `TextSplitter`(带重叠、超长硬切兜底)
- 知识点提取流水线:`DocPipeline` → `KnowledgeExtractor` → 合并去重 → `KnowledgeReport`(Markdown)
- 结构化输出:调用时开启 JSON mode(`response_format=json_object`),取代正则清洗
- 统一 JSON 入口:Spring `@Bean ObjectMapper` 单例 + 强类型请求/响应 DTO(`llm/dto`)
- 应用配置外置:`config.properties`,经 Spring `@Value` 注入
- 调用日志:按天追加到 `logs/api-YYYY-MM-DD.log`(`ApiLogger`,手写)
- 命令行入口:`Main`(对话)、`ExtractMain`(文档提取)

### 变更 / Changed
- `SessionStore` 由静态工具类改为 Spring 管理的 bean(依赖注入,便于替换与代理)
- 文档管线与提取器接入 Spring 容器,统一依赖装配
- LLM 调用引入 `ChatOptions`(可扩展的调用参数;目前含 jsonMode)

### 修复 / Fixed
- `deepseek.maxTokens` 由 20000 修正为 **8192**(`deepseek-chat` 的单次输出上限)
- 非 2xx 响应与缺少 `choices` 时给出明确错误(此前可能解析为空 / NPE)
- 补齐各模块单元测试至 **54 个**(含 Spring 容器冒烟测试)

## [0.1.0] - 2026-09-17

### 新增 / Added
- 初始化 Maven 项目骨架(`groupId: io.github.likeelysia`,`artifactId: formal-agent`)
- 配置 JUnit 5 测试依赖与 `maven-compiler-plugin` / `maven-surefire-plugin`
- 添加 MIT 许可证、README、`.gitignore`(含密钥防护规则)
