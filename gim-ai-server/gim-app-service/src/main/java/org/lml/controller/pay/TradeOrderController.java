package org.lml.controller.pay;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.lml.common.result.CommonResult;
import org.lml.service.pay.ITradeOrderService;
import org.lml.thirdService.order.entity.convertor.TradeOrderConvertor;
import org.lml.thirdService.order.entity.dto.TradeOrder;
import org.lml.thirdService.order.entity.resp.TradeOrderVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 * 交易订单表 前端控制器
 * </p>
 *
 * @author lml
 * @since 2026-03-12
 */
@RestController
@RequestMapping("/pay/tradeOrder")
public class TradeOrderController {

    @Resource
    private ITradeOrderService itradeOrderService;

    /**
     * 查询支付订单列表
     */
    @GetMapping("/getPayOrderList")
    public CommonResult<Page<TradeOrderVO>> getPayOrderList(@RequestParam("pageNo") Integer pageNo, @RequestParam("pageSize") Integer pageSize) {


        String userId = StpUtil.getLoginId().toString();

        //这里只查询订单状态为：待支付、支付成功、关单状态
        Page<TradeOrder> page = new Page<>(pageNo, pageSize);
        QueryWrapper<TradeOrder> qw = new QueryWrapper<>();
        qw.eq("buyer_id",userId);
        qw.in("order_state","CONFIRM","PAID","CLOSED");
        Page<TradeOrder> list = itradeOrderService.page(page,qw);

        List<TradeOrderVO> dataList = TradeOrderConvertor.INSTANCE.mapToVo(list.getRecords());

        Page<TradeOrderVO> pageVO = new Page<>();
        pageVO.setRecords(dataList);
        pageVO.setTotal(list.getTotal());
        pageVO.setCurrent(pageNo);
        pageVO.setSize(pageSize);

        return CommonResult.successResponse(pageVO);
    }

    /**
     * 查询支付订单详情
     */
    @GetMapping("/getPayOrderDetail")
    public CommonResult<TradeOrderVO> getPayOrderDetail(@RequestParam("tradeOrderId") String tradeOrderId) {

        String userId = StpUtil.getLoginId().toString();
        QueryWrapper<TradeOrder> qw = new QueryWrapper<>();
        qw.eq("order_id",tradeOrderId);
        qw.eq("buyer_id",userId);
        TradeOrder tradeOrder = itradeOrderService.getOne(qw);
        return CommonResult.successResponse(TradeOrderConvertor.INSTANCE.mapToVo(tradeOrder));
    }


}
