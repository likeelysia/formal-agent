package io.github.likeelysia.formalagent.doc;

/** 切块后的文本块,继承它所属段落的出处。 */
public record TextChunk(String text, String location) {
}