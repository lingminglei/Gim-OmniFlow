package org.lml.utils;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.StrUtil;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * 生成二维码工具包
 */
@Component
public class QrCodeUtil {

    private static final Logger log = LoggerFactory.getLogger(QrCodeUtil.class);

    /**
     * 默认宽度
     */

    private static final int WIDTH = 700;
    /**
     * 默认高度
     */

    private static final int HEIGHT = 300;
    /**
     * 默认文件格式
     */

    private static final String FORMAT = "png";
    /**
     * 一些默认参数
     */

    private static final Map<EncodeHintType, Object> HINTS = new HashMap();

    static {
        // 字符编码
        HINTS.put(EncodeHintType.CHARACTER_SET, "utf-8");
        // 容错等级 L、M、Q、H 其中 L 为最低, H 为最高
        HINTS.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        // 二维码与图片边距
        HINTS.put(EncodeHintType.MARGIN, 2);
    }

    /**
     * 生成图片的条形码
     *
     * @param content 内容
     * @param paths   路径
     */
    public static void generateBarCodeFile(String content, String paths) throws WriterException {

        Code128Writer writer = new Code128Writer();
        BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.CODE_128, WIDTH, HEIGHT, HINTS);
        Path path = Paths.get(paths);
        if (!StrUtil.isEmpty(content)) {
            try {
                MatrixToImageWriter.writeToPath(bitMatrix, FORMAT, path);
            } catch (IOException e) {
                log.error("生成条形码失败：{}", e.getMessage());
            }
        }
    }


    /**
     * 生成文件流的条形码
     *
     * @param content  内容
     * @param response 响应体
     * @return 流
     */
    public static OutputStream generateBarCodeStream(String content, HttpServletResponse response) throws WriterException {


        Code128Writer writer = new Code128Writer();
        OutputStream outputStream = null;
        BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.CODE_128, WIDTH, HEIGHT, HINTS);
        if (!StrUtil.isEmpty(content)) {
            try {
                // 字节输出流
                outputStream = response.getOutputStream();
                MatrixToImageWriter.writeToStream(bitMatrix, FORMAT, outputStream);
                return outputStream;
            } catch (IOException e) {
                log.error("生成条形码失败：{}", e.getMessage());
            } finally {
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        log.error("流关闭失败：{}", e.getMessage());
                    }
                }
            }
        }
        return null;
    }

    /**
     * 生成base64的二维码
     *
     * @param content 内容
     * @return base64二维码
     */
    public static String generateBarCodeBase64(String content) throws WriterException {

        String base64;
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        Code128Writer writer = new Code128Writer();
        BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.CODE_128, WIDTH, HEIGHT, HINTS);
        if (!StrUtil.isEmpty(content)) {
            try {
                BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
                ImageIO.write(bufferedImage, FORMAT, os);
                base64 = Base64.encode(os.toByteArray());
                return base64;
            } catch (Exception e) {
                log.error("生成二维码失败：{}", e.getMessage());
            } finally {
                try {
                    os.flush();
                    os.close();
                } catch (IOException e) {
                    log.error("流关闭失败：{}", e.getMessage());
                }

            }
        }
        return null;
    }


    /**
     * 生成图片的二维码
     *
     * @param content 内容
     * @param paths   路径
     */
    public static void generateQrCodeFile(String content, String paths) {

        MultiFormatWriter writer = new MultiFormatWriter();
        if (!StrUtil.isEmpty(content)) {
            try {
                // 字节输出流
                BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, WIDTH, HEIGHT, HINTS);
                Path path = Paths.get(paths);
                MatrixToImageWriter.writeToPath(bitMatrix, FORMAT, path);
            } catch (Exception e) {
                log.error("生成二维码失败：{}", e.getMessage());
            }
        }

    }


    /**
     * 生成文件流的二维码
     *
     * @param content  内容
     * @param response 响应体
     * @return 流
     */
    public static OutputStream generateQrCodeStream(String content, HttpServletResponse response) {

        MultiFormatWriter writer = new MultiFormatWriter();
        OutputStream outputStream = null;
        if (!StrUtil.isEmpty(content)) {
            try {
                // 字节输出流
                BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, WIDTH, HEIGHT, HINTS);
                outputStream = response.getOutputStream();
                MatrixToImageWriter.writeToStream(bitMatrix, FORMAT, outputStream);
                return outputStream;
            } catch (Exception e) {
                log.error("生成二维码失败：{}", e.getMessage());
            } finally {
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        log.error("流关闭失败：{}", e.getMessage());
                    }
                }
            }
        }
        return null;
    }


    /**
     * 生成base64的二维码
     *
     * @param content 内容
     * @return base64二维码
     */
    public static String generateQrCodeBase64(String content) {

        String base64;
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        if (!StrUtil.isEmpty(content)) {
            try {
                QRCodeWriter writer = new QRCodeWriter();
                BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, WIDTH, HEIGHT, HINTS);
                BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
                ImageIO.write(bufferedImage, FORMAT, os);
                base64 = Base64.encode(os.toByteArray());
                return base64;
            } catch (Exception e) {
                log.error("生成二维码失败：{}", e.getMessage());
            } finally {
                try {
                    os.flush();
                    os.close();
                } catch (IOException e) {
                    log.error("流关闭失败：{}", e.getMessage());
                }

            }
        }
        return null;
    }


}

