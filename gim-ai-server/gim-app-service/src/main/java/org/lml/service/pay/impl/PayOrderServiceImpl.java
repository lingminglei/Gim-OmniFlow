package org.lml.service.pay.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.lml.mapper.pay.PayOrderMapper;
import org.lml.service.pay.IPayOrderService;
import org.lml.thirdService.pay.entity.dto.PayOrder;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 支付单表 服务实现类
 * </p>
 *
 * @author lml
 * @since 2026-03-12
 */
@Service
public class PayOrderServiceImpl extends ServiceImpl<PayOrderMapper, PayOrder> implements IPayOrderService {

}
