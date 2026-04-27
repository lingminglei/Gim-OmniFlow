package org.lml.business.billing.domain.tcc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.lml.common.dto.BaseEntity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 积分 TCC 事务记录。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("billing_tcc_record")
public class BillingTccRecord extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * TCC 事务号，唯一标识一笔积分预扣减事务。
     */
    private String tccTransactionNo;

    /**
     * 用户 ID，表示本次扣减针对哪个用户。
     */
    private String userId;

    /**
     * 业务单号，用于关联外部订单、任务或其他业务请求。
     */
    private String bizNo;

    /**
     * Try 阶段的请求号，用于整个 TCC 事务的首次幂等控制。
     */
    private String requestNo;

    /**
     * Confirm 阶段的请求号，用于确认扣减幂等控制。
     */
    private String confirmRequestNo;

    /**
     * Cancel 阶段的请求号，用于取消扣减幂等控制。
     */
    private String cancelRequestNo;

    /**
     * 本次 TCC 事务涉及的积分数量。
     */
    private BigDecimal amount;

    /**
     * 当前事务状态，例如 TRYING、CONFIRMED、CANCELED。
     */
    private String status;

    /**
     * 事务过期时间，超过该时间仍未确认时可进入自动补偿扫描。
     */
    private Date expireTime;

    /**
     * 来源系统，标识这笔事务由哪个模块发起。
     */
    private String sourceSystem;

    /**
     * 事务备注，用于记录业务说明或补偿原因。
     */
    private String remark;

    /**
     * 补偿重试次数，用于控制自动补偿的重试过程。
     */
    private Integer retryCount;

    /**
     * 最近一次执行失败的错误信息，便于排查问题。
     */
    private String lastErrorMsg;
}
