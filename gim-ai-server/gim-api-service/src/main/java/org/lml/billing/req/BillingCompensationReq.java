package org.lml.billing.req;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 人工触发补偿请求。
 */
@Data
public class BillingCompensationReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 幂等请求号。
     */
    @NotBlank(message = "requestNo 不能为空")
    private String requestNo;

    /**
     * 来源系统。
     */
    @NotBlank(message = "sourceSystem 不能为空")
    private String sourceSystem;

    /**
     * 业务单号，和 tccTransactionNo 至少传一个。
     */
    private String bizNo;

    /**
     * TCC 事务号，和 bizNo 至少传一个。
     */
    private String tccTransactionNo;

    /**
     * 备注。
     */
    private String remark;
}
