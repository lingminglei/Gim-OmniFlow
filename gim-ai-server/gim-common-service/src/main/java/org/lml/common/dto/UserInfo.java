package org.lml.common.dto;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * <p>
 * 用户信息表
 * </p>
 *
 * @author lml
 * @since 2025-08-06
 */
@TableName("user_info")
@Data
public class UserInfo extends BaseEntity implements Serializable  {

    private static final long serialVersionUID = 1L;

    /**
     * 用户唯一ID（可用于生成唯一标识）
     */
    private String userId;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 加密后的密码
     */
    private String passWord;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 头像URL
     */
    private String avatarUrl;

    /**
     * 个性签名
     */
    private String userIntroduction;

    /**
     * 在线状态
     */
    private String status;

    /**
     * 最后一次在线时间
     */
    private Date lastOnlineTime;

}
