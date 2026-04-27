package org.lml.business.billing.domain.account.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.lml.common.dto.BaseEntity;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 积分账户实体，对应 `user_assets` 表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_assets")
public class BillingAccount extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户 ID，用于标识积分归属的用户。
     */
    private String userId;

    /**
     * 可用积分余额，表示当前用户还能直接消费的积分数量。
     */
    private BigDecimal balance;

    /**
     * 冻结积分余额，表示已经预扣减但尚未最终确认的积分数量。
     */
    private BigDecimal frozenBalance;

    /**
     * 已使用的磁盘空间字节数，兼容旧表中的资产扩展字段。
     */
    private Long diskUsedBytes;

    /**
     * 磁盘总空间字节数，兼容旧表中的资产扩展字段。
     */
    private Long diskLimitBytes;
}
