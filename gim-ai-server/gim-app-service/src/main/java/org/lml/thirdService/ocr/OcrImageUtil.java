package org.lml.thirdService.ocr;

import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Size;

/**
 * 图片处理工具类: 最终可用版
 */
public class OcrImageUtil {

    /**
     * 自动预处理图片以提高 OCR 识别率
     * @param inputPath 原图路径
     * @param outputPath 预处理后路径
     */
    public static void autoPreprocess(String inputPath, String outputPath) {
        Mat src = opencv_imgcodecs.imread(inputPath);
        if (src.empty()) throw new IllegalArgumentException("无法读取图片：" + inputPath);

        // 灰度
        Mat gray = new Mat();
        opencv_imgproc.cvtColor(src, gray, opencv_imgproc.COLOR_BGR2GRAY);

        // 小图放大
        if (gray.cols() < 800) {
            double scale = 800.0 / gray.cols();
            opencv_imgproc.resize(gray, gray, new Size(0, 0), scale, scale, opencv_imgproc.INTER_CUBIC);
        }

        // 边缘检测（用来判断清晰度/噪声）
        Mat edges = new Mat();
        opencv_imgproc.Canny(gray, edges, 50, 150);
        double edgeDensity = opencv_core.countNonZero(edges) / (double) (edges.rows() * edges.cols());

        Mat processed = new Mat();

        if (edgeDensity > 0.02) {
            // 🔹 噪声较多 / 复杂背景 → 重预处理
            System.out.println("[OCR] 使用重预处理");
            opencv_imgproc.medianBlur(gray, processed, 3);
            opencv_imgproc.adaptiveThreshold(processed, processed, 255,
                    opencv_imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                    opencv_imgproc.THRESH_BINARY, 15, 10);
        } else {
            // 🔹 清晰图 → 轻预处理
            System.out.println("[OCR] 使用轻预处理");
            opencv_imgproc.threshold(gray, processed, 0, 255,
                    opencv_imgproc.THRESH_BINARY | opencv_imgproc.THRESH_OTSU);
        }

        // 确保黑字白底（Tesseract 习惯黑字白底）
        double whiteRatio = opencv_core.countNonZero(processed) / (double) (processed.rows() * processed.cols());
        if (whiteRatio < 0.5) {
            opencv_core.bitwise_not(processed, processed);
        }

        opencv_imgcodecs.imwrite(outputPath, processed);

        // 释放资源
        src.release();
        gray.release();
        edges.release();
        processed.release();
    }
}
