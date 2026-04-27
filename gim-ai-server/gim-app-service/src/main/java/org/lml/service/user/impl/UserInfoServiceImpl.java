package org.lml.service.user.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.lml.common.dto.UserInfo;
import org.lml.common.dto.UserLoginReq;
import org.lml.common.result.CommonResult;
import org.lml.entity.dto.user.UserAssets;
import org.lml.entity.resp.user.UserRanksInfoResp;
import org.lml.mapper.user.UserAssetsMapper;
import org.lml.mapper.user.UserInfoMapper;
import org.lml.service.user.IUserInfoService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.math.BigDecimal;
import java.util.Date;

import static org.lml.common.constant.CommonConstant.BASEAVATAR;
import static org.lml.utils.CommonUtils.generateRandomUsername;
import static org.lml.utils.PasswordUtils.encryptPassword;
import static org.lml.utils.ServerConfig.getServerIp;

/**
 * <p>
 * 用户信息表 服务实现类
 * </p>
 *
 * @author lml
 * @since 2025-08-06
 */
@Service
@Slf4j
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements IUserInfoService {


    @Resource
    private UserInfoMapper userInfoMapper;

    @Resource
    private UserAssetsMapper userAssetsMapper;


    /**
     * 1、判断用户信息是否存在。
     *
     * 1、写入用户信息。
     *
     * 2、写入用户资产表信息。
     *
     * @param userLoginReq
     * @return
     */
    @Override
    public UserInfo doRegister(UserLoginReq userLoginReq) {

        UserInfo userInfo = new UserInfo();
        BeanUtil.copyProperties(userLoginReq,userInfo);

        userInfo.setUserName(generateRandomUsername());//生成随机用户名
        userInfo.setUserId(IdUtil.simpleUUID());//用户ID
        userInfo.setAvatarUrl(BASEAVATAR);//用户默认用户头像

        userInfo.setPassWord(encryptPassword(userLoginReq.getPassWord()));//对密码进行加密
        userInfoMapper.insert(userInfo);

        // 处理用户资产信息
        UserAssets  userAssets = new UserAssets();
        userAssets.setUserId(userInfo.getUserId());
        userAssets.setBalance(BigDecimal.ZERO);//可用积分初始化
        userAssets.setFrozenBalance(BigDecimal.ZERO);//冻结积分
        userAssets.setDiskUsedBytes(0l);//网盘已用空间
        userAssets.setDiskLimitBytes(1024L * 1024L * 1024L);//网盘总容量
        userAssetsMapper.insert(userAssets);
        return userInfo;
    }

    @Override
    public CommonResult<UserRanksInfoResp> getRanksInfo() {

        log.info("query getRanksInfo==");

        String userId = String.valueOf(StpUtil.getLoginId());

        log.info("userId==={}",userId);

        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id",userId);
        UserInfo userInfo = userInfoMapper.selectOne(queryWrapper);

        ObjectUtil.isNull(userInfo);

        if(ObjectUtil.isNull(userInfo)){
            return CommonResult.errorResponse("用户不存在，请稍后尝试！");
        }

        QueryWrapper<UserAssets> queryWrapper1 = new QueryWrapper<>();
        queryWrapper1.eq("user_id",userId);
        UserAssets userAssets = userAssetsMapper.selectOne(queryWrapper1);


        UserRanksInfoResp userRanksInfoResp = new UserRanksInfoResp();
        BeanUtil.copyProperties(userInfo,userRanksInfoResp);
        BeanUtil.copyProperties(userAssets,userRanksInfoResp);

        userRanksInfoResp.setServerIp(getServerIp());

        Date beg = userInfo.getCreateTime();
        // 获取当前时间
        Date end = new Date();
        // 计算相差的天数
        long diffDays = DateUtil.between(beg, end, cn.hutool.core.date.DateUnit.DAY);

        userRanksInfoResp.setDatesCnt((int) diffDays);
        return CommonResult.successResponse(userRanksInfoResp);
    }
}
