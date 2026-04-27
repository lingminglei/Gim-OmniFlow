package org.lml.thirdService.ocr;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import java.awt.image.BufferedImage;
import java.io.File;

public class TestOcr {

    /**
     * 直接原始使用：Tesseract 做
     * @param args
     */
    public static void main1(String[] args) {
        // 1. 图片文件
        File imageFile = new File("D:\\file\\data1.png");

        // 2. 创建 Tesseract 实例
        ITesseract tesseract = new Tesseract();

        // 3. 设置 tessdata 路径（Tesseract OCR 的语言包目录）
        // Windows 示例：
        tesseract.setDatapath("D:\\developSoftware\\RAG\\tesseract-ocr\\tessdata");
        // Linux/Mac 可默认安装路径

        // 4. 可选：设置语言，比如中文用 "chi_sim"
        tesseract.setLanguage("chi_sim+eng");

        try {
            // 5. 执行 OCR 识别
            String result = tesseract.doOCR(imageFile);

            // 6. 输出识别文本
            System.out.println("识别结果：");
            System.out.println(result);
        } catch (TesseractException e) {
            System.err.println(e.getMessage());
        }
    }


    /**
     * 最终可用版
     * @param args
     */
    public static void main2(String[] args) {
        String input = "D:\\file\\data1.png";
        String output = "D:\\file\\data1_pre.png";

        OcrImageUtil.autoPreprocess(input, output);

        // 然后把 output 传给 Tesseract 识别
        ITesseract tesseract = new Tesseract();
        tesseract.setDatapath("D:\\developSoftware\\RAG\\tesseract-ocr\\tessdata");
        tesseract.setLanguage("chi_sim+eng");
        try {
            String result = tesseract.doOCR(new File(output));
            System.out.println("识别结果：\n" + result);
        } catch (TesseractException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        String input = "D:\\file\\data3.png";

        // 这里 saveDebugImage=true 只是方便调试时查看效果
        BufferedImage preImage = OcrImageUtil1.autoPreprocessToImage(input, true, "D:\\file\\data3_pre.png");

        ITesseract tesseract = new Tesseract();
        tesseract.setDatapath("D:\\developSoftware\\RAG\\tesseract-ocr\\tessdata");
        tesseract.setLanguage("chi_sim+eng");

        String result = tesseract.doOCR(preImage);
        System.out.println("识别结果：\n" + result);
    }
}
