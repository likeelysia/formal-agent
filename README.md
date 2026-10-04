# Formal Agent

> 我的第一个开源 AI Agent 项目 —— 边学边建,公开记录。
> My first open-source AI Agent project — building it in public while learning.

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](./LICENSE)
![Status](https://img.shields.io/badge/status-early%20dev-orange)
![Java](https://img.shields.io/badge/Java-17+-blue)

---

## 项目简介 / About

**中文**:这是我学习 Java 后端 + AI Agent 开发过程中做的第一个开源项目。
目标是做一个「文档 → 知识点」的 Agent:把教材/讲义喂进来,自动读取文档、切块、
调用大模型提取知识点,最后产出一份 Markdown 清单;同时支持与模型的普通多轮对话。

**English**: My first open-source project, built while learning Java backend development
and AI Agent engineering. It reads a document, splits it into chunks, calls an LLM to
extract knowledge points, and renders a Markdown report. It also supports plain
multi-turn chat with the model.

> **当前状态 / Status**: 早期开发中(`0.1.0-SNAPSHOT`)。**核心流水线已跑通**:
> 多轮对话、会话存档、文档 → 知识点提取、结构化输出、Spring IoC 装配。
> Early stage. The core pipeline works: multi-turn chat, session persistence,
> document → knowledge extraction, structured output, and Spring IoC wiring.

---

## 功能特性 / Features

- **多轮对话**:保留历史上下文,与 DeepSeek 循环对话
- **会话存档**:Java 原生序列化(`.bin`)+ 可读 JSON 导出(`.json`)
- **文档 → 知识点**:读取 `.txt` / `.md` → 带重叠切块 → 逐块提取 → 合并去重 → Markdown 报告
- **结构化输出**:调用时开启 JSON mode(`response_format=json_object`),让模型直接产出合法 JSON
- **配置外置**:`config.properties` + Spring 注入
- **调用日志**:按天追加到 `logs/api-YYYY-MM-DD.log`
- **Spring IoC 装配**:统一 `ObjectMapper`、各服务由容器管理
- **单元测试**:54 个,含 Spring 容器冒烟测试(不联网、秒级)

---

## 技术栈 / Tech Stack

| 项目 | 版本 | 说明 |
|---|---|---|
| Java | 17+ | `maven.compiler.release=17`,任何 JDK 17 及以上都能编译 |
| Maven | 3.9+ | 构建与依赖管理(含 Maven Wrapper) |
| Spring Context | 5.3.15 | IoC 容器(依赖注入;`@Component` / `@Bean` / `@Value`) |
| Jackson | 2.21.2 | JSON 序列化 / 反序列化 |
| JUnit | 5.10.2 | 单元测试 |
| 大模型 | DeepSeek | `deepseek-chat`(OpenAI 兼容的 `/chat/completions`) |

---

## 快速开始 / Quick Start

### 前置要求(Prerequisites)

- **JDK 17+**
- **Maven 3.9+**(或直接用自带的 `mvnw` / `mvnw.cmd`)
- 环境变量 **`DEEPSEEK_API_KEY`**(从 [platform.deepseek.com](https://platform.deepseek.com) 获取)

```powershell
# 设置密钥(Windows PowerShell;设置后需重开终端才生效)
setx DEEPSEEK_API_KEY "你的key"
```

### 构建与测试

```bash
mvn clean test        # 跑测试
mvn clean package     # 打包(产物在 target/ 下)
```

### 跑起来

```bash
# 1) 先把依赖复制到 target/dependency(只需一次)
mvn dependency:copy-dependencies -DoutputDirectory=target/dependency

# 2) 普通对话(多轮;输入 exit 退出,退出时自动存档到 sessions/)
java -cp "target/classes;target/dependency/*" io.github.likeelysia.formalagent.cli.Main

# 3) 文档 → 知识点(不给输出路径时,默认写到 <输入文件名>.knowledge.md)
java -cp "target/classes;target/dependency/*" io.github.likeelysia.formalagent.cli.ExtractMain "你的文件.md"
```

> classpath 分隔符:Windows 用 `;`,Linux / macOS 用 `:`。

---

## 目录结构 / Project Layout

```
src/main/java/io/github/likeelysia/formalagent/
├── chat/       # ChatSession、Message —— 会话与消息
├── cli/        # 命令行入口:Main(对话)、ExtractMain(提取)
├── config/     # AppConfig、SpringConfig —— 配置与容器装配
├── doc/        # DocumentReader / PlainTextReader / TextSplitter / DocPipeline
├── extract/    # KnowledgeExtractor / ExtractService / KnowledgeReport —— 知识点提取
├── llm/        # LlmClient 接口 + DeepSeekClient 实现(+ dto/)
├── log/        # ApiLogger —— 手写调用日志(后续换 Logback)
├── service/    # ChatService —— 业务编排
└── store/      # SessionStore —— 存档 / 读档 / 导出
```

标准 Maven 目录约定:`src/main/java` 放主代码,`src/test/java` 放测试 —— Maven 自动识别,无需额外配置。

---

## 关于密钥 / About Secrets

本项目会调用大模型 API。**仓库中不会包含任何 API Key**:

- 密钥通过**环境变量** `DEEPSEEK_API_KEY` 提供;
- `.gitignore` 已屏蔽 `.env`、`*.local.properties`、`secrets/`、`*.pem`、`*.key` 等敏感文件。

This project calls LLM APIs. **No API keys are committed**: the key is read from the
`DEEPSEEK_API_KEY` environment variable, and `.gitignore` blocks common secret files.

---

## 路线图 / Roadmap

- [x] 打通 HTTP 与 DeepSeek(Jackson 解析)
- [x] 多轮对话 + 会话存档 + 配置外置 + 调用日志
- [x] 文档 → 知识点提取流水线
- [x] Spring IoC 接入 + 统一 ObjectMapper / DTO
- [x] 结构化输出(JSON mode)
- [ ] 超时 + 重试 + 退避
- [ ] Spring Boot 化(配置分层 / 日志框架 / 可执行 jar)
- [ ] 工具调用(Function Calling)+ ReAct
- [ ] 真 RAG(embedding + 向量检索)

---

## 许可证 / License

[MIT](./LICENSE) © 2026 likeelysia
