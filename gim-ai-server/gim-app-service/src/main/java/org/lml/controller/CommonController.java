package org.lml.controller;

import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.lml.common.result.CommonResult;
import org.lml.service.CommonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RequestMapping("/commonApi")
@RestController
@Slf4j
public class CommonController {

    @Autowired
    private CommonService commonService;

    /**
     * GET /api/captcha -> 返回验证码 Key 和 Base64 图片
     */
    @GetMapping("/captcha")
    public CommonResult<Map<String, String>> getCaptchaImage(@RequestParam("phone")String phone) {

        if(ObjectUtils.isNull(phone) || StringUtils.isBlank(phone)){
            return CommonResult.errorResponse("手机号不能为空");
        }
        Map<String, String> captchaData = commonService.generateCaptcha(phone);
        // R 是你自定义的统一返回结果封装类
        return CommonResult.successResponse(captchaData);
    }
}
