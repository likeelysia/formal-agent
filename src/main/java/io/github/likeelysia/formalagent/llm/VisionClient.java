package io.github.likeelysia.formalagent.llm;

import java.nio.file.Path;

/** 视觉客户端:给一张图 + 一段指令,还我模型的文字回复。 */
public interface VisionClient {
    String ask(Path image, String prompt);
}