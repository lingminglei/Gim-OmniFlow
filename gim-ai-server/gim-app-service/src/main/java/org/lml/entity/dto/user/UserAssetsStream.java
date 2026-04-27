package org.lml.entity.dto.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.lml.common.dto.BaseEntity;
import org.lml.common.enums.pay.UserAssetsBizType;
import org.lml.thirdService.pay.constant.PayChannel;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * <p>
 * 用户资产变动流水表
 * </p>
 *
 * @author lml
 * @since 2026-03-12
 */
@TableName("user_assets_stream")
@Data
public class UserAssetsStream extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 变动积分(正数为增加，负数为减少)
     */
    private BigDecimal amount;

    /**
     * 业务类型: RECHARGE(充值), REFUND(退还)
     */
    private UserAssetsBizType bizType;

    /**
     * 关联业务单号(如订单号、任务ID)
     */
    private String bizNo;

    /**
     * TCC事务号
     */
    private String transactionId;

    /**
     * 支付单号
     */
    private String payOrderId;

    /**
     * 套餐编码
     */
    private String packageCode;

    /**
     * 支付渠道
     */
    private PayChannel channel;

    /**
     * 变动前余额
     */
    private BigDecimal balanceBefore;

    /**
     * 变动后余额
     */
    private BigDecimal balanceAfter;

    /**
     * 备注信息
     */
    private String remark;

    /**
     * 数据一致性哈希签名(防止记录被篡改)
     */
    private String dataHash;
}
