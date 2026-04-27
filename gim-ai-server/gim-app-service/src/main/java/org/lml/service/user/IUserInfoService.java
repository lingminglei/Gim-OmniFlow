package org.lml.service.user;

import com.baomidou.mybatisplus.extension.service.IService;
import org.lml.common.dto.UserInfo;
import org.lml.common.dto.UserLoginReq;
import org.lml.common.result.CommonResult;
import org.lml.entity.resp.user.UserRanksInfoResp;

/**
 * <p>
 * 用户信息表 服务类
 * </p>
 *
 * @author lml
 * @since 2025-08-06
 */
public interface IUserInfoService extends IService<UserInfo> {


    /**
     * 用户注册
     */
    public UserInfo doRegister(UserLoginReq userLoginReq);

    /**
     *
     */
    public CommonResult<UserRanksInfoResp> getRanksInfo();
}
