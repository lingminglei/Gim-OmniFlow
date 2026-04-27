package org.lml.billing.req;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 积分余额查询请求。
 */
@Data
public class BillingBalanceQueryReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户 ID。
     */
    @NotBlank(message = "userId 不能为空")
    private String userId;
}
