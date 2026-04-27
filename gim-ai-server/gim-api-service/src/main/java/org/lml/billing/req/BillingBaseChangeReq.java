package org.lml.billing.req;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 积分变更类请求的公共字段。
 */
@Data
public class BillingBaseChangeReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 幂等请求号。
     */
    @NotBlank(message = "requestNo 不能为空")
    private String requestNo;

    /**
     * 用户 ID。
     */
    @NotBlank(message = "userId 不能为空")
    private String userId;

    /**
     * 业务单号。
     */
    @NotBlank(message = "bizNo 不能为空")
    private String bizNo;

    /**
     * 来源系统。
     */
    @NotBlank(message = "sourceSystem 不能为空")
    private String sourceSystem;

    /**
     * 变更积分数量，必须为正整数。
     */
    @NotNull(message = "amount 不能为空")
    @Min(value = 1, message = "amount 必须大于 0")
    private Integer amount;

    /**
     * 备注。
     */
    private String remark;
}
