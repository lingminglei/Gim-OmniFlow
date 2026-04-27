package org.lml.service.user.facade;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.lml.common.dto.UserInfo;
import org.lml.common.dto.UserLoginReq;
import org.lml.common.result.CommonResult;
import org.lml.service.CommonService;
import org.lml.service.user.IUserInfoService;
import org.lml.AiGateWay.api.UserFacadeService;

import javax.annotation.Resource;
import java.util.Date;

import static org.lml.common.enums.CommonEnum.USER_INFO_OFFLINE_STATUS;
import static org.lml.common.enums.CommonEnum.USER_INFO_ONLINE_STATUS;
import static org.lml.utils.PasswordUtils.checkPassword;

@DubboService(version = "1.0.0")
@Slf4j
public class UserFacadeServiceImpl implements UserFacadeService {

    @Resource
    private IUserInfoService userInfoService;

    @Resource
    private CommonService commonService;

    @Override
    public CommonResult doLogin(UserLoginReq userLoginReq) {
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

        userInfo.setStatus(USER_INFO_ONLINE_STATUS.getValue());
        userInfo.setLastOnlineTime(new Date());

        userInfoService.updateById(userInfo);

        return CommonResult.successResponse(userInfo,"登录成功");
    }

    @Override
    public CommonResult<String> logout(String userId) {
        StpUtil.logout(userId);

        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id",userId);

        UserInfo userInfo = userInfoService.getOne(queryWrapper);

        userInfo.setStatus(USER_INFO_OFFLINE_STATUS.getValue());

        userInfoService.updateById(userInfo);

        log.info("当前登录用户:{}，退出登录成功！",userId);
        return CommonResult.successResponse("注销成功");
    }
}
