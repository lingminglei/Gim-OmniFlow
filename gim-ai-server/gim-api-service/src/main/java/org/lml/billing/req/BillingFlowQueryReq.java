package org.lml.billing.req;

import lombok.Data;
import org.lml.billing.enums.BillingChangeType;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 积分流水查询请求。
 */
@Data
public class BillingFlowQueryReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户 ID。
     */
    @NotBlank(message = "userId 不能为空")
    private String userId;

    /**
     * 页码，从 1 开始。
     */
    @NotNull(message = "pageNo 不能为空")
    @Min(value = 1, message = "pageNo 必须大于 0")
    private Long pageNo;

    /**
     * 每页条数。
     */
    @NotNull(message = "pageSize 不能为空")
    @Min(value = 1, message = "pageSize 必须大于 0")
    private Long pageSize;

    /**
     * 业务单号，可选。
     */
    private String bizNo;

    /**
     * 请求号，可选。
     */
    private String requestNo;

    /**
     * TCC 事务号，可选。
     */
    private String tccTransactionNo;

    /**
     * 来源系统，可选。
     */
    private String sourceSystem;

    /**
     * 流水类型，可选。
     */
    private BillingChangeType changeType;
}
