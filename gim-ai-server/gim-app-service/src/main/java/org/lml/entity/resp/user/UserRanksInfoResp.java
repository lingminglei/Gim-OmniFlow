package org.lml.entity.resp.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class UserRanksInfoResp {

    /**
     * 用户名
     */
    private String userName;

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
     * 最后一次在线时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastOnlineTime;

    /**
     * 可用积分余额
     */
    private BigDecimal balance;

    /**
     * 服务IP
     */
    private String serverIp;

    /**
     * 注册天数
     */
    private Integer datesCnt;
}
