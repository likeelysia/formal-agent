package io.github.likeelysia.formalagent.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.likeelysia.formalagent.agent.AgentResult;
import io.github.likeelysia.formalagent.agent.AgentService;
import io.github.likeelysia.formalagent.exception.AgentException;
import io.github.likeelysia.formalagent.llm.VisionClient;
import io.github.likeelysia.formalagent.service.FileStorage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

/** 多图上传这条链路的单元测试:张数上限、逐张识别、单张失败不拖垮整批。 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AgentController 多图上传")
class AgentControllerTest {

    @Mock
    private AgentService agentService;

    @Mock
    private VisionClient vision;

    @Mock
    private FileStorage fileStorage;

    private AgentController controller;

    @BeforeEach
    void setUp() {
        controller = new AgentController(agentService, vision, fileStorage);
        when(agentService.run(anyString())).thenReturn(new AgentResult("答案", List.of("search_knowledge({})"), 1));
    }

    private static MockMultipartFile image(String name) {
        return new MockMultipartFile("files", name, "image/jpeg", ("fake-" + name).getBytes());
    }

    @Test
    @DisplayName("超过 10 张 -> 直接拒绝,不碰视觉模型")
    void rejectsTooManyImages() {
        List<MultipartFile> eleven = new ArrayList<>();
        for (int i = 1; i <= AgentController.MAX_IMAGES + 1; i++) {
            eleven.add(image("p" + i + ".jpg"));
        }

        assertThatThrownBy(() -> controller.agentWithImage(eleven, null))
                .isInstanceOf(AgentException.class)
                .hasMessageContaining("最多");

        verify(vision, never()).ask(any(), anyString());
        verify(agentService, never()).run(anyString());
    }

    @Test
    @DisplayName("没有图片 -> 报错")
    void rejectsEmptyFiles() {
        assertThatThrownBy(() -> controller.agentWithImage(List.of(), null))
                .isInstanceOf(AgentException.class);
    }

    @Test
    @DisplayName("多张图 -> 逐张识别,并按【第 n 张】拼进最终提问")
    void mergesMultipleImagesInOrder() {
        when(fileStorage.store(anyString(), any())).thenReturn(Path.of("stored.jpg"));
        when(vision.ask(any(), anyString())).thenReturn("一盏台灯", "一本书");

        AgentController.AgentResponse res = controller.agentWithImage(
                List.of(image("a.jpg"), image("b.jpg")), "这两张图有什么共同点?");

        verify(vision, times(2)).ask(any(), anyString());
        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(agentService).run(prompt.capture());

        assertThat(prompt.getValue())
                .contains("这两张图有什么共同点?")
                .contains("【第 1 张】").contains("一盏台灯")
                .contains("【第 2 张】").contains("一本书");

        assertThat(res.answer()).isEqualTo("答案");
        assertThat(res.steps()).isEqualTo(1);
    }

    @Test
    @DisplayName("某一张识别失败 -> 跳过它,其余照样回答")
    void keepsGoingWhenOneImageFails() {
        when(fileStorage.store(anyString(), any())).thenReturn(Path.of("stored.jpg"));
        when(vision.ask(any(), anyString()))
                .thenThrow(new RuntimeException("429 限流"))
                .thenReturn("第二张是公式");

        controller.agentWithImage(List.of(image("bad.jpg"), image("good.jpg")), null);

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(agentService).run(prompt.capture());
        assertThat(prompt.getValue())
                .contains("【第 1 张】").contains("识别失败")
                .contains("【第 2 张】").contains("第二张是公式");
    }

    @Test
    @DisplayName("全部识别失败且没写问题 -> 报错,不拿空内容去问模型")
    void failsWhenNothingRecognizedAndNoQuestion() {
        when(fileStorage.store(anyString(), any())).thenReturn(Path.of("stored.jpg"));
        when(vision.ask(any(), anyString())).thenThrow(new RuntimeException("模型挂了"));

        assertThatThrownBy(() -> controller.agentWithImage(List.of(image("bad.jpg")), null))
                .isInstanceOf(AgentException.class)
                .hasMessageContaining("没能识别");

        verify(agentService, never()).run(anyString());
    }

    @Test
    @DisplayName("全部识别失败但用户自己写了问题 -> 仍然让 Agent 回答")
    void stillAnswersWhenUserWroteAQuestion() {
        when(fileStorage.store(anyString(), any())).thenReturn(Path.of("stored.jpg"));
        when(vision.ask(any(), anyString())).thenThrow(new RuntimeException("模型挂了"));

        controller.agentWithImage(List.of(image("bad.jpg")), "这张图是什么意思?");

        verify(agentService).run(anyString());
    }
}
