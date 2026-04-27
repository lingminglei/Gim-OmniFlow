package org.lml.business.billing.domain.flow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.lml.common.dto.BaseEntity;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 积分流水实体，对应 `user_assets_stream` 表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_assets_stream")
public class BillingFlow extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户 ID，表示这条流水属于哪个用户。
     */
    private String userId;

    /**
     * 积分变动数量。
     * 正数通常表示充值或返还，业务含义由 changeType 决定。
     */
    private BigDecimal amount;

    /**
     * 兼容旧表字段，保留历史业务类型标识。
     */
    private String bizType;

    /**
     * 业务单号，用于关联外部业务请求或订单。
     */
    private String bizNo;

    /**
     * 兼容旧表的 transaction_id 字段，用于记录历史事务号。
     */
    private String transactionId;

    /**
     * 幂等请求号，用于保证同一业务请求不会重复记账。
     */
    private String requestNo;

    /**
     * 来源系统，标识是谁调用了积分账务服务。
     */
    private String sourceSystem;

    /**
     * 流水变更类型，例如充值、预扣减、确认扣减、取消扣减等。
     */
    private String changeType;

    /**
     * 变更前的可用积分余额。
     */
    private BigDecimal balanceBefore;

    /**
     * 变更后的可用积分余额。
     */
    private BigDecimal balanceAfter;

    /**
     * 变更前的冻结积分余额。
     */
    private BigDecimal frozenBefore;

    /**
     * 变更后的冻结积分余额。
     */
    private BigDecimal frozenAfter;

    /**
     * 流水备注，用于记录业务语义或补偿原因。
     */
    private String remark;

    /**
     * 流水摘要哈希，用于校验关键账务数据是否被篡改。
     */
    private String dataHash;

    /**
     * TCC 事务号，用于关联同一笔预扣减事务的 Try/Confirm/Cancel。
     */
    private String tccTransactionNo;

    /**
     * 兼容旧表字段，历史上用于记录支付单号，当前 billing 模块不直接使用。
     */
    @TableField("pay_order_id")
    private String payOrderId;
}
