# Formal Agent

> 我的第一个开源 AI Agent 项目 —— 边学边建,公开记录。
> My first open-source AI Agent project — building it in public while learning.

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](./LICENSE)
![Status](https://img.shields.io/badge/status-WIP-orange)
![Java](https://img.shields.io/badge/Java-17+-blue)

---

## 📌 项目简介 / About

**中文**:这是我学习 Java 后端 + AI Agent 开发过程中,打算认真做下去的第一个开源项目。
目标是把"教材/文档 → 知识点提取"这件事,做成一个能自己跑的 Agent(后续会加入大模型调用、工具调用与多轮对话)。

**English**: This is my first serious open-source project, built while learning Java backend development and AI Agent engineering.
The goal is an agent that turns textbooks/documents into structured knowledge points (LLM calls, tool calling and multi-turn dialogue will be added step by step).

> ⚠️ **当前状态 / Status**: 早期开发中(`0.1.0-SNAPSHOT`),代码尚未开始,项目骨架先行。
> Early stage (`0.1.0-SNAPSHOT`). The project skeleton is in place; source code is coming.

---

## 🛠 技术栈 / Tech Stack

| 项目 | 版本 | 说明 |
|---|---|---|
| Java | 17+ | `maven.compiler.release=17`,任何 JDK 17 及以上都能编译 |
| Maven | 3.9+ | 构建与依赖管理(含 Maven Wrapper) |
| JUnit | 5.10.2 | 单元测试 |
| 大模型 API | 待定 | 规划中(DeepSeek / Kimi 等) |

---

## 🚀 快速开始 / Quick Start

前置要求(Prerequisites):**JDK 17+**、**Maven 3.9+**(或直接用自带的 Maven Wrapper)。

```bash
# 克隆仓库 / Clone
git clone https://github.com/likeelysia/formal-agent.git
cd formal-agent

# 编译 / Compile
mvn clean compile

# 跑测试 / Run tests
mvn clean test

# 打包 / Package(产物在 target/ 下)
mvn clean package
```

Windows 上也可以把 `mvn` 换成 `mvnw.cmd`(Maven Wrapper,无需本机安装 Maven)。

---

## 📁 目录结构 / Project Layout

```
formal-agent/
├── src/
│   ├── main/
│   │   ├── java/          # 主代码
│   │   └── resources/     # 配置文件、资源
│   └── test/
│       └── java/          # 单元测试
├── .mvn/                  # Maven Wrapper
├── pom.xml                # Maven 项目配置
├── LICENSE                # MIT 许可证
├── CHANGELOG.md           # 版本变更记录
└── README.md
```

标准 Maven 目录约定:`src/main/java` 放主代码,`src/test/java` 放测试代码 —— Maven 会自动识别,不用在 `pom.xml` 里额外配置。

---

## 🔐 关于密钥 / About Secrets

本项目会调用大模型 API。**仓库中不会包含任何 API Key**:

- 本地密钥通过**环境变量**或**不纳入版本控制的配置文件**提供;
- `.gitignore` 已屏蔽 `.env`、`*.local.properties`、`config.properties` 等敏感文件。

This project calls LLM APIs. **No API keys are committed**: use environment variables or git-ignored config files locally.

---

## 📄 许可证 / License

[MIT](./LICENSE) © 2026 likeelysia
