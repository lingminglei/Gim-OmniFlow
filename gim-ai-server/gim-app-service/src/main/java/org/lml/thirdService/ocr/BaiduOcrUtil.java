package org.lml.thirdService.ocr;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Base64;

/**
 * 百度 Ocr调用工具包
 */
public class BaiduOcrUtil {

    // 百度 API Key 和 Secret Key
    private static final String API_KEY = "zISmcIFKlnt7JKD5o41aziqn";
    private static final String SECRET_KEY = "RhnQmCsROdaqdmKqZd7KY0TP6Mr6JJEb";

    /**
     * 获取百度 Access Token
     */
    public static String getAccessToken() throws IOException {
        String authHost = "https://aip.baidubce.com/oauth/2.0/token";
        String getAccessTokenUrl = authHost
                + "?grant_type=client_credentials"
                + "&client_id=" + API_KEY
                + "&client_secret=" + SECRET_KEY;

        URL realUrl = new URL(getAccessTokenUrl);
        HttpURLConnection connection = (HttpURLConnection) realUrl.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();

        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                result.append(line);
            }
            JSONObject jsonObject = JSON.parseObject(result.toString());
            return jsonObject.getString("access_token");
        }
    }

    /**
     * 调用百度通用文字识别（高精度版）
     */
    public static String recognizeMixedLang(String imagePath) throws IOException {
        String accessToken = getAccessToken();
        String url = "https://aip.baidubce.com/rest/2.0/ocr/v1/accurate_basic";

        // 读取图片转 Base64
        byte[] imgData = readFileByBytes(imagePath);
        String imgStr = Base64.getEncoder().encodeToString(imgData);
        String imgParam = URLEncoder.encode(imgStr, "UTF-8");

        // 发送 POST 请求
        String param = "image=" + imgParam + "&language_type=CHN_ENG"; // 自动中英文混合
        HttpURLConnection connection = (HttpURLConnection) new URL(url + "?access_token=" + accessToken).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setDoOutput(true);
        try (OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream())) {
            out.write(param);
            out.flush();
        }

        // 读取响应
        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                result.append(line);
            }

            JSONObject jsonObject = JSON.parseObject(result.toString());
            JSONArray wordsResult = jsonObject.getJSONArray("words_result");

            StringBuilder finalText = new StringBuilder();
            for (int i = 0; i < wordsResult.size(); i++) {
                finalText.append(wordsResult.getJSONObject(i).getString("words")).append("\n");
            }
            return finalText.toString();
        }
    }

    private static byte[] readFileByBytes(String filePath) throws IOException {
        File file = new File(filePath);
        try (InputStream is = new FileInputStream(file); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = is.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            return bos.toByteArray();
        }
    }

    public static void main(String[] args) throws IOException {
        String imagePath = "D:\\file\\data3.png";
        String result = recognizeMixedLang(imagePath);
        System.out.println("中英文混合识别结果：\n" + result);
    }

}
