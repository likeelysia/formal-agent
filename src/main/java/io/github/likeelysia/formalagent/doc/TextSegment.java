package io.github.likeelysia.formalagent.doc;

/** 一段带"出处"的文本。location = 位置(如"第 3 页");没有位置概念时为 null。 */
public record TextSegment(String text, String location) {
}