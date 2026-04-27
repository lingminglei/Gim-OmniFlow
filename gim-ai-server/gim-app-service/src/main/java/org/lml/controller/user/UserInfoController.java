package org.lml.controller.user;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.lml.common.dto.UserInfo;
import org.lml.common.dto.UserLoginReq;
import org.lml.common.result.CommonResult;
import org.lml.entity.resp.user.UserRanksInfoResp;
import org.lml.service.CommonService;
import org.lml.service.user.IUserInfoService;
import org.lml.utils.RedisUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.lml.common.constant.RedisCacheConstant.PAY_TOKEN_INFO;
import static org.lml.common.constant.RedisCacheConstant.PAY_TOKEN_INFO_TIME;
import static org.lml.common.enums.CommonEnum.USER_INFO_OFFLINE_STATUS;
import static org.lml.common.enums.CommonEnum.USER_INFO_ONLINE_STATUS;
import static org.lml.utils.PasswordUtils.checkPassword;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserInfoController {


    @Resource
    private IUserInfoService userInfoService;

    @Resource
    private CommonService commonService;

    @Resource
    private RedisUtils redisUtils;

    /**
     * 用户登录
     * @return
     */
    @PostMapping("/doLogin")
    public CommonResult doLogin(@RequestBody @Validated UserLoginReq userLoginReq) {

        log.info("gim-service-doLogin");

        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("phone",userLoginReq.getPhone());

        UserInfo userInfo = userInfoService.getOne(queryWrapper);

        if(ObjectUtil.isNull(userInfo)){
            //用户未注册，自动跳转到登录操作
            userInfo = userInfoService.doRegister(userLoginReq);
        }

        if(!checkPassword(userLoginReq.getPassWord(),userInfo.getPassWord())){
            return CommonResult.errorResponse("账号或密码错误");
        }

        Boolean isValue = commonService.validateCaptcha(userLoginReq.getPhone(),
                userLoginReq.getCaptcha());

        if(isValue.equals(Boolean.valueOf(false))){
            return CommonResult.errorResponse("验证码错误");
        }

        StpUtil.login(userInfo.getUserId());

        userInfo.setStatus(USER_INFO_ONLINE_STATUS.getValue());
        userInfo.setLastOnlineTime(new Date());

        userInfoService.updateById(userInfo);

        String token = StpUtil.getTokenValue();

        Map map = new HashMap();
        map.put("token",token);
        map.put("userInfo",userInfo);
        return CommonResult.successResponse(map,"登录成功");

    }

    @GetMapping("/isLogin")
    public String isLogin() {
        String userId = String.valueOf(StpUtil.getLoginId());
        log.info("当前登录用户：{}", userId);
        return "当前会话是否登录：" + StpUtil.isLogin();
    }

    @GetMapping("/logout")
    public CommonResult<String> logout(@RequestParam("userId") String userId) {
        StpUtil.logout(userId);

        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id",userId);

        UserInfo userInfo = userInfoService.getOne(queryWrapper);

        userInfo.setStatus(USER_INFO_OFFLINE_STATUS.getValue());

        userInfoService.updateById(userInfo);

        log.info("当前登录用户:{}，退出登录成功！",userId);
        return CommonResult.successResponse("注销成功");
    }

    /**
     * 获取用户积分信息
     */
    @GetMapping("/getRanksInfo")
    public CommonResult<UserRanksInfoResp> getRanksInfo() {
        return userInfoService.getRanksInfo();
    }

    /**
     * 获取支付Token
     */
    @GetMapping("/getPayToken")
    public CommonResult<String> getPayToken() {

        String useId = String.valueOf(StpUtil.getLoginId());

        // 2. 生成简单的 32 位唯一幂等号
        String idempotentKey = IdUtil.fastSimpleUUID();

        String tokenKey = PAY_TOKEN_INFO + StpUtil.getLoginId();

        //将支付token 放入Redis,用于下单校验；
        redisUtils.set(tokenKey, idempotentKey,PAY_TOKEN_INFO_TIME);

        return CommonResult.successResponse(idempotentKey);
    }

}
