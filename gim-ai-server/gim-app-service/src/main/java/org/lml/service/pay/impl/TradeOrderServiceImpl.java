package org.lml.service.pay.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.lml.mapper.pay.TradeOrderMapper;
import org.lml.service.pay.ITradeOrderService;
import org.lml.thirdService.order.entity.dto.TradeOrder;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 交易订单表 服务实现类
 * </p>
 *
 * @author lml
 * @since 2026-03-12
 */
@Service
public class TradeOrderServiceImpl extends ServiceImpl<TradeOrderMapper, TradeOrder> implements ITradeOrderService {

}
