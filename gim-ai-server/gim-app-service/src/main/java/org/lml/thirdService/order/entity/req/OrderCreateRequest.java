package org.lml.thirdService.order.entity.req;

import lombok.Getter;
import lombok.Setter;
import org.lml.thirdService.order.entity.event.TradeOrderEvent;
import org.lml.thirdService.pay.constant.UserType;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * @author Hollis
 */
@Getter
@Setter
public class OrderCreateRequest {

    /**
     * 买家id
     */
    @NotNull(message = "买家id不能为空")
    private String buyerId;

    /**
     * 买家id类型
     */
    private UserType buyerType = UserType.CUSTOMER;

    /**
     * 卖家id
     */
    @NotNull(message = "卖家id不能为空")
    private String sellerId;

    /**
     * 卖家id类型
     */
    private UserType sellerType = UserType.PLATFORM;

    /**
     * 订单金额
     */
    @DecimalMin(value = "0.0", inclusive = false, message = "订单金额必须大于0")
    private BigDecimal orderAmount;

    /**
     * 商品Id
     */
    @NotNull(message = "商品Id不能为空")
    private String goodsId;

    /**
     * 商品图片
     */
    private String goodsPicUrl;

    /**
     * 商品名称
     */
    private String goodsName;

    /**
     * 商品数量
     */
    @Min(value = 1)
    private int itemCount;

    /**
     * 商品单价
     */
    @DecimalMin(value = "0.0", inclusive = false, message = "商品单价必须大于0")
    private BigDecimal itemPrice;

    /**
     * 快照版本
     */
    private Integer snapshotVersion;

    /**
     * 交易订单号
     */
    private String orderId;

}

