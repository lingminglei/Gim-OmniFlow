package org.lml.thirdService.order.entity.req;


import lombok.Getter;
import lombok.Setter;
import org.lml.thirdService.order.entity.event.TradeOrderEvent;
import org.lml.thirdService.pay.constant.UserType;

import javax.validation.constraints.NotNull;
import java.util.Date;

/**
 * @author Hollis
 */
@Setter
@Getter
public abstract class BaseOrderUpdateRequest{

    /**
     * 订单id
     */
    @NotNull(message = "orderId 不能为空")
    private String orderId;

    /**
     * 操作时间
     */
    @NotNull(message = "operateTime 不能为空")
    private Date operateTime;

    /**
     * 操作人
     */
    @NotNull(message = "operator 不能为空")
    private String operator;

    /**
     * 操作人类型
     */
    @NotNull(message = "operatorType 不能为空")
    private UserType operatorType;

    /**
     * 操作幂等号
     */
    @NotNull(message = "identifier 不能为空")
    private String identifier;


    /**
     * 获取订单事件
     *
     * @return
     */
    public TradeOrderEvent getOrderEvent() {
        return null;
    }

}
