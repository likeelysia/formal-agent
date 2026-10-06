package io.github.likeelysia.formalagent.doc;

import io.github.likeelysia.formalagent.exception.AgentException;
import io.github.likeelysia.formalagent.llm.VisionClient;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

/**
 * PDF 读取器,两种策略:
 * ① 有文字层(出版方原生 PDF)→ 直接按页抽文字;
 * ② 没有文字层(扫描件/影印版)→ 逐页渲染成图片,交给视觉模型 OCR。
 * 出处统一标到页码。
 */
@Component
public class PdfReader implements DocumentReader {

    /** OCR 渲染精度(DPI):越高越清晰,但图越大越慢越贵 */
    private static final float OCR_DPI = 200f;

    /** 页与页之间的停顿(毫秒):规避 Moonshot「组织并发上限 = 1」限流 */
    private static final long OCR_PAGE_GAP_MS = 1500;

    private static final String OCR_PROMPT =
            "请提取这张图片中的所有文字,原样输出;不要翻译、不要解释、不要添加任何说明。"
                    + "如果图中没有文字,只输出一个空字符串。";

    private final VisionClient vision;

    public PdfReader(VisionClient vision) {
        this.vision = vision;
    }

    @Override
    public List<TextSegment> read(Path file) {
        if (file == null) throw new AgentException("文件路径不能为空");
        if (!Files.isRegularFile(file)) throw new AgentException("这不是一个文件:" + file);

        try (PDDocument doc = Loader.loadPDF(file.toFile())) {
            List<TextSegment> segments = extractText(doc);          // ① 先试文字层
            if (segments.isEmpty()) {
                segments = ocrPages(doc);                           // ② 扫描件兜底
            }
            if (segments.isEmpty()) {
                throw new AgentException("PDF 里没有可提取的内容(文字层为空,OCR 也没识别到):" + file);
            }
            return segments;
        } catch (IOException e) {
            throw new AgentException("读取 PDF 失败:" + file, e);
        }
    }

    /** 策略①:按页抽文字层 */
    private List<TextSegment> extractText(PDDocument doc) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        List<TextSegment> segments = new ArrayList<>();
        for (int p = 1; p <= doc.getNumberOfPages(); p++) {
            stripper.setStartPage(p);
            stripper.setEndPage(p);
            String text = stripper.getText(doc);
            if (!text.isBlank()) segments.add(new TextSegment(text, "第 " + p + " 页"));
        }
        return segments;
    }

    /** 策略②:逐页渲染成 JPEG → 视觉模型 OCR */
    private List<TextSegment> ocrPages(PDDocument doc) throws IOException {
        PDFRenderer renderer = new PDFRenderer(doc);
        List<TextSegment> segments = new ArrayList<>();
        List<Integer> failed = new ArrayList<>();
        int pages = doc.getNumberOfPages();
        for (int i = 0; i < pages; i++) {
            if (i > 0) sleepQuietly(OCR_PAGE_GAP_MS);          // ← 页间停顿,规避并发限流
            BufferedImage image = renderer.renderImageWithDPI(i, OCR_DPI);
            Path tmp = Files.createTempFile("pdf-page-", ".jpg");
            try {
                ImageIO.write(image, "jpg", tmp.toFile());
                String text = vision.ask(tmp, OCR_PROMPT);
                int len = (text == null) ? 0 : text.trim().length();
                System.out.println("[OCR] 第 " + (i + 1) + "/" + pages + " 页 → " + len + " 字");
                if (len > 0) {
                    segments.add(new TextSegment(text, "第 " + (i + 1) + " 页"));
                }
            } catch (AgentException e) {                        // ← 单页失败不再拖垮整本
                failed.add(i + 1);
                System.out.println("[OCR] 第 " + (i + 1) + " 页失败,已跳过:" + e.getMessage());
            } finally {
                Files.deleteIfExists(tmp);
            }
        }
        if (!failed.isEmpty()) {
            System.out.println("[OCR] ⚠ 有 " + failed.size() + " 页没成功:" + failed + "(可稍后单独重跑)");
        }
        return segments;
    }

    private static void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}