package org.lml.billing.resp;

import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 积分流水分页响应。
 */
@Data
public class BillingFlowPageResp implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long pageNo;

    private Long pageSize;

    private Long total;

    private List<BillingFlowResp> records = Collections.emptyList();
}
