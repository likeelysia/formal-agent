# Formal Agent

> 我的第一个开源 AI Agent 项目 —— 边学边建,公开记录。
> My first open-source AI Agent project — building it in public while learning.

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](./LICENSE)
![Status](https://img.shields.io/badge/status-early%20dev-orange)
![Java](https://img.shields.io/badge/Java-17+-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F)

---

## 项目简介 / About

**中文**:这是我学习 Java 后端 + AI Agent 开发过程中做的第一个开源项目。
它要把「教材」变成「可问答的知识库」:

1. **入库**:把教材(PDF / 图片 / 文本)读进来 —— 扫描版 PDF 走**视觉模型 OCR**,
   原生 PDF 直接抽文字层 —— 切块后交给大模型**提取知识点**(带页码出处),落到知识库;
2. **问答**:给一张图片(或提问),先识别内容,再**从知识库里按语义检索**相关知识点,
   最后让模型**只依据这些知识点**用大白话回答,并**标明出处**;
3. 同时支持与模型的普通多轮对话。

**English**: My first open-source AI Agent project. It ingests textbooks (text-layer PDFs via
`PDFTextStripper`, scanned PDFs / images via a vision model OCR), extracts knowledge points
(with page-level provenance) into a knowledge base, then answers questions **grounded on
semantic (embedding) retrieval** over that knowledge base — with citations.

> **当前状态 / Status**: `0.1.0-SNAPSHOT`。**核心闭环已跑通**(入库 + 向量检索 + 带出处问答),
> 并已完成 **Spring Boot 工程化**(配置类型安全 / profile / 可执行 jar)。
> Core loop works (ingest → vector retrieval → grounded QA with citations); Spring Boot
> engineering (typed config / profiles / executable jar) is done.

---

## 功能特性 / Features

- **文档入库(三种来源)**:`.txt` / `.md` 文本、原生 PDF(文字层)、**扫描版 PDF / 图片(视觉 OCR)**;产出带页码出处的知识点
- **语义检索(真 RAG 的第一步)**:知识点 → **embedding 向量**,检索按**余弦相似度**取 top-K;
  embedding 服务不可用时**自动降级**为关键词匹配(不会因此搜不了)
- **带出处的问答**:图片 → 识图 → 检索 → 「只依据知识点」生成回答 + 【出处】
- **多轮对话**:保留历史上下文,与 DeepSeek 循环对话
- **会话存档**:Java 原生序列化(`.bin`)+ 可读 JSON 导出(`.json`)
- **结构化输出**:调用时开启 JSON mode(`response_format=json_object`)
- **健壮性**:超时 + **重试 + 退避**(Spring Retry / AOP);音频类长任务失败可跳过
- **配置**:`application.yml` + `@ConfigurationProperties`(类型安全)+ **profile**
- **调用日志**:按天追加到 `logs/api-YYYY-MM-DD.log`
- **Spring Boot 工程化**:可执行 fat jar(`java -jar`)、内嵌日志(SLF4J/Logback)
- **单元测试**:**74 个**,含 Boot 容器冒烟测试与配置绑定测试(不联网、秒级)

---

## 技术栈 / Tech Stack

| 项目 | 版本 | 说明 |
|---|---|---|
| Java | 17+ | 编译 `release=17`,JDK 17 及以上均可 |
| Maven | 3.9+ | 构建与依赖管理(含 Maven Wrapper) |
| **Spring Boot** | **3.5.16** | 自动配置 / 可执行 jar / 内嵌日志(底层 Spring 6) |
| Spring Retry | (Boot BOM 管) | 声明式重试(`@Retryable` + AOP) |
| Jackson | (Boot BOM 管) | JSON 序列化 / 反序列化 |
| Apache PDFBox | 3.0.8 | PDF 文字层提取 + 逐页渲染(供 OCR) |
| JUnit | (Boot BOM 管) | 单元测试 |
| **DeepSeek** | `deepseek-chat` | 文本对话 / 知识点提炼 |
| **Kimi 视觉** | `kimi-k2.6` | 扫描件 OCR / 图片理解(请求里关闭"思考模式") |
| **本地 embedding** | `bge-small-zh-v1.5` | 经 llama.cpp `llama-server` 提供 OpenAI 兼容接口 |

---

## 快速开始 / Quick Start

### 前置要求(Prerequisites)

- **JDK 17+**
- **Maven 3.9+**(或用自带的 `mvnw` / `mvnw.cmd`)
- 环境变量:
  - **`DEEPSEEK_API_KEY`** —— 文本对话 / 提炼([platform.deepseek.com](https://platform.deepseek.com))
  - **`MOONSHOT_API_KEY`** —— 视觉 OCR / 图片问答([platform.moonshot.cn](https://platform.moonshot.cn))

```powershell
# Windows PowerShell(设置后需重开终端 / IDEA 才生效)
setx DEEPSEEK_API_KEY "你的key"
setx MOONSHOT_API_KEY "你的key"
```

### (可选但推荐)启动本地 embedding 服务

语义检索依赖一个本地 embedding 服务。**不启动也能跑** —— 只是检索会降级为关键词匹配。

```powershell
# 需要:llama.cpp 的 llama-server.exe + bge-small-zh 模型(路径见脚本内注释)
start-embedding.bat          # 起在 http://127.0.0.1:8090
```

### 构建与运行

```bash
mvn clean test               # 跑测试(74 个)
mvn clean package            # 打包 → target/formal-agent-0.1.0-SNAPSHOT.jar

# 可执行 jar(推荐)
java -jar target/formal-agent-0.1.0-SNAPSHOT.jar

# 带 profile(dev 会打开调试输出)
java -jar target/formal-agent-0.1.0-SNAPSHOT.jar --spring.profiles.active=dev
```

也可以在 IDEA 里直接运行 `FormalAgentApplication`(绿色 ▶)。

### 命令行怎么用

启动后是交互式提示符:

- **直接说话** = 普通多轮对话
- **粘 `.md` / `.txt` / `.pdf`** = 提取知识点(写一份 `<文件名>.knowledge.md`)
- **粘图片(`.png` / `.jpg`)** = 识别图片 → **对照知识库回答(带出处)**
- 输入 `exit` 退出(自动存档会话)

> 批量入库(把教材灌进 `knowledge/base.json`)目前用 `cli.IngestMain`,在 IDEA 里运行;
> 后续会做成 HTTP 接口(见路线图)。

---

## 目录结构 / Project Layout

```
src/main/java/io/github/likeelysia/formalagent/
├── FormalAgentApplication.java   # Spring Boot 启动类
├── chat/       # ChatSession、Message —— 会话与消息
├── cli/        # ChatCli(交互式命令行,CommandLineRunner)、IngestMain、ExtractMain、FileHint
├── config/     # AppConfig(@ConfigurationProperties)、SpringConfig —— 配置与容器装配
├── doc/        # DocumentReader / PlainTextReader / PdfReader(文字层+OCR) / ImageReader / TextSplitter / DocPipeline
├── extract/    # KnowledgeExtractor / ExtractService / IngestService / KnowledgeReport —— 提取与入库
├── llm/        # LlmClient、VisionClient、EmbeddingClient(+ 各自实现与 dto/)
├── knowledge/  # KnowledgeItem / KnowledgeStore / JsonKnowledgeStore —— 知识库与向量检索
├── log/        # ApiLogger —— 模型调用审计日志
├── service/    # ChatService(对话)、QaService(图片问答)
└── store/      # SessionStore —— 存档 / 读档 / 导出
```

配置在 `src/main/resources/application.yml`(前缀 `fa.*`)。

---

## 关于密钥 / About Secrets

本项目会调用大模型 API。**仓库中不会包含任何 API Key**:

- 密钥通过**环境变量**提供(`DEEPSEEK_API_KEY` / `MOONSHOT_API_KEY`);
- `.gitignore` 已屏蔽 `.env`、`*.local.properties`、`secrets/`、`*.pem`、`*.key` 等敏感文件;
- 知识库数据(`knowledge/`)与调用日志(`logs/`)同样不入库。

This project calls LLM APIs. **No API keys are committed**: keys come from environment
variables, and `.gitignore` blocks common secret files as well as generated data.

---

## 路线图 / Roadmap

- [x] 打通 HTTP 与 DeepSeek(Jackson 解析)
- [x] 多轮对话 + 会话存档 + 配置外置 + 调用日志
- [x] 文档 → 知识点提取流水线
- [x] Spring IoC 接入 + 统一 ObjectMapper / DTO
- [x] 结构化输出(JSON mode)
- [x] 超时 + 重试 + 退避(Spring Retry)
- [x] 知识库(接口 `KnowledgeStore`)+ 入库流水线(带页码出处)
- [x] 扫描版 PDF / 图片 OCR(Kimi 视觉)
- [x] 图片问答(识图 → 检索 → 带出处回答)
- [x] **真检索:embedding + 余弦相似度 top-K**(本地 llama.cpp)
- [x] **Spring Boot 化:可执行 jar / `@ConfigurationProperties` / profile**
- [ ] 日志规范(SLF4J/Logback 全量替换 `println`)
- [ ] HTTP 接口(REST:问答 / 入库 / 健康检查)
- [ ] 工具调用(Function Calling)+ ReAct
- [ ] 向量库 / 阈值 + hybrid(向量 + 关键词)
- [ ] Prompt 模板外置

---

## 许可证 / License

[MIT](./LICENSE) © 2026 likeelysia
