package org.lml.service.pay.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.lml.mapper.pay.TradeOrderStreamMapper;
import org.lml.service.pay.ITradeOrderStreamService;
import org.lml.thirdService.order.entity.dto.TradeOrderStream;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 订单变动流水记录表 服务实现类
 * </p>
 *
 * @author lml
 * @since 2026-03-12
 */
@Service
public class TradeOrderStreamServiceImpl extends ServiceImpl<TradeOrderStreamMapper, TradeOrderStream> implements ITradeOrderStreamService {

}
