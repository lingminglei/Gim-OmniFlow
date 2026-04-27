package org.lml.thirdService.pay.entity.dto.convertor;

import org.lml.thirdService.pay.entity.dto.PayOrder;
import org.lml.thirdService.pay.entity.req.PayCreateRequest;
import org.lml.thirdService.pay.entity.resp.PayOrderVO;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * @author Hollis
 */
@Mapper(nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface PayOrderConvertor {

    PayOrderConvertor INSTANCE = Mappers.getMapper(PayOrderConvertor.class);

    /**
     * 转换实体
     *
     * @param request
     * @return
     */
    public PayOrder mapToEntity(PayCreateRequest request);

    /**
     * 转换vo
     *
     * @param request
     * @return
     */
    public PayOrderVO mapToVo(PayOrder request);

    /**
     * 转换vo
     *
     * @param request
     * @return
     */
    public List<PayOrderVO> mapToVo(List<PayOrder> request);
}
