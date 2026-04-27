package org.lml.thirdService.order.entity.convertor;

import org.lml.thirdService.order.entity.dto.TradeOrder;
import org.lml.thirdService.order.entity.req.OrderCreateRequest;
import org.lml.thirdService.order.entity.resp.TradeOrderVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 交易订单转换器。
 * 使用 MapStruct 生成字段拷贝代码。
 */
@Mapper(
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface TradeOrderConvertor {

    TradeOrderConvertor INSTANCE = Mappers.getMapper(TradeOrderConvertor.class);

    /**
     * 把下单请求转换为交易订单实体。
     *
     * @param request 下单请求
     * @return 交易订单实体
     */
    TradeOrder mapToEntity(OrderCreateRequest request);

    /**
     * 把交易订单实体转换为前端返回对象。
     *
     * @param request 交易订单实体
     * @return 前端返回对象
     */
    @Mapping(target = "buyerName", ignore = true)
    @Mapping(target = "sellerName", ignore = true)
    @Mapping(target = "goodsPicUrl", ignore = true)
    @Mapping(target = "timeout", expression = "java(request.isTimeout())")
    @Mapping(target = "payExpireTime", expression = "java(request.getPayExpireTime())")
    TradeOrderVO mapToVo(TradeOrder request);

    /**
     * 批量把交易订单实体转换为前端返回对象列表。
     *
     * @param request 交易订单实体列表
     * @return 前端返回对象列表
     */
    List<TradeOrderVO> mapToVo(List<TradeOrder> request);
}
