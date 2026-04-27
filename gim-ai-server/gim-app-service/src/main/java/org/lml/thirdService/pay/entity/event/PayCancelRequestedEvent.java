package org.lml.thirdService.pay.entity.event;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
/**
 * 支付取消请求事件。
 * 由 cancel 接口发送给 MQ，再由消费端执行异步回滚。
 */
public class PayCancelRequestedEvent {
    private String transactionId;
    private String payOrderId;
    private String bizNo;
    private int retryCount;
    private String reason;
}
