package org.lml.thirdService.ocr;

import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Size;

import java.awt.image.BufferedImage;

/**
 * 增强版图片处理工具
 */
public class OcrImageUtil1 {

    /**
     * 自动预处理并返回 BufferedImage（不落地到磁盘）
     */
    public static BufferedImage autoPreprocessToImage(String inputPath, boolean saveDebugImage, String debugPath) {
        Mat src = opencv_imgcodecs.imread(inputPath);
        if (src.empty()) throw new IllegalArgumentException("无法读取图片：" + inputPath);

        Mat gray = new Mat();
        opencv_imgproc.cvtColor(src, gray, opencv_imgproc.COLOR_BGR2GRAY);

        // 小图放大
        if (gray.cols() < 800) {
            double scale = 800.0 / gray.cols();
            opencv_imgproc.resize(gray, gray, new Size(0, 0), scale, scale, opencv_imgproc.INTER_CUBIC);
        }

        Mat edges = new Mat();
        opencv_imgproc.Canny(gray, edges, 50, 150);
        double edgeDensity = opencv_core.countNonZero(edges) / (double) (edges.rows() * edges.cols());

        Mat processed = new Mat();
        if (edgeDensity > 0.02) {
            System.out.println("[OCR] 使用重预处理");
            opencv_imgproc.medianBlur(gray, processed, 3);
            opencv_imgproc.adaptiveThreshold(processed, processed, 255,
                    opencv_imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                    opencv_imgproc.THRESH_BINARY, 15, 10);
        } else {
            System.out.println("[OCR] 使用轻预处理");
            opencv_imgproc.threshold(gray, processed, 0, 255,
                    opencv_imgproc.THRESH_BINARY | opencv_imgproc.THRESH_OTSU);
        }

        // 黑字白底
        double whiteRatio = opencv_core.countNonZero(processed) / (double) (processed.rows() * processed.cols());
        if (whiteRatio < 0.5) {
            opencv_core.bitwise_not(processed, processed);
        }

        // 如果需要调试保存
        if (saveDebugImage && debugPath != null) {
            opencv_imgcodecs.imwrite(debugPath, processed);
        }

        // 转 BufferedImage 直接给 Tesseract
        return matToBufferedImage(processed);
    }

    private static BufferedImage matToBufferedImage(Mat mat) {
        int type = BufferedImage.TYPE_BYTE_GRAY;
        if (mat.channels() > 1) {
            Mat mat2 = new Mat();
            opencv_imgproc.cvtColor(mat, mat2, opencv_imgproc.COLOR_BGR2RGB);
            type = BufferedImage.TYPE_3BYTE_BGR;
            mat = mat2;
        }
        byte[] b = new byte[mat.channels() * mat.cols() * mat.rows()];
        mat.data().get(b);
        BufferedImage image = new BufferedImage(mat.cols(), mat.rows(), type);
        image.getRaster().setDataElements(0, 0, mat.cols(), mat.rows(), b);
        return image;
    }
}
