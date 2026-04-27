package org.lml.service;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import lombok.extern.slf4j.Slf4j;
import org.lml.utils.RedisUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class CommonService {

    @Resource
    private RedisUtils redisUtils;

    // 验证码有效期（秒）
    private static final long CAPTCHA_EXPIRATION = 60 * 5;

    /**
     * 生成验证码图片、键和值
     * @return 包含 key (UUID) 和 base64 图片字符串的 Map
     */
    public Map<String, String> generateCaptcha(String phone) {

        // 1. 生成验证码对象
        // 随机生成 4 位数字字母混合验证码
        LineCaptcha lineCaptcha = CaptchaUtil.createLineCaptcha(120, 48, 4, 10);

        String captchaCode = lineCaptcha.getCode(); // 验证码值 (e.g., "ABCD")

        // 2. 存储验证码值到 Redis (Key: UUID, Value: CaptchaCode)
        String redisKey = "captcha:" + phone;
        redisUtils.set(redisKey, captchaCode,5*60);

        String base64Img = lineCaptcha.getImageBase64();

        Map<String, String> result = new HashMap<>();
        result.put("key", phone);
        result.put("img", base64Img);
        result.put("captchaCode",captchaCode);
        return result;
    }

    /**
     * 验证用户输入的验证码是否正确
     * @param key 验证码 Key
     * @param code 用户输入的验证码
     * @return 验证是否通过
     */
    public boolean validateCaptcha(String key, String code) {
        String redisKey = "captcha:" + key;
        String storedCode = (String) redisUtils.get(redisKey);

        log.info("校验验证码！redisKey={}，手机号：{}，原验证码：{}，用户验证码：{}",redisKey,key,storedCode,code);

        if (storedCode == null) {
            return false; // 验证码已过期或不存在
        }
        // 验证完成后删除缓存中的验证码，防止重复使用
        redisUtils.del(redisKey);

        // 不区分大小写验证
        return storedCode.equalsIgnoreCase(code);
    }
}
