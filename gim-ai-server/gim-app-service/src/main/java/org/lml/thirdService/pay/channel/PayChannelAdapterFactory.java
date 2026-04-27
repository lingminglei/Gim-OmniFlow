package org.lml.thirdService.pay.channel;

import lombok.extern.slf4j.Slf4j;
import org.lml.thirdService.pay.constant.PayChannel;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
/**
 * 支付渠道适配器工厂。
 * 启动时收集所有适配器，运行时按支付渠道路由。
 */
public class PayChannelAdapterFactory {
    /**
     * 支付渠道和适配器的映射关系。
     */
    private final Map<PayChannel, PayChannelAdapter> adapters = new EnumMap<>(PayChannel.class);

    /**
     * 初始化支付渠道适配器工厂并收集全部可用适配器。
     *
     * @param adapterList Spring注入的渠道适配器列表
     */
    public PayChannelAdapterFactory(List<PayChannelAdapter> adapterList) {
        for (PayChannelAdapter adapter : adapterList) {
            adapters.put(adapter.getChannel(), adapter);
        }
        log.info("支付渠道适配器工厂初始化完成，已支持渠道={}", adapters.keySet());
    }

    /**
     * 按支付渠道获取对应的适配器实现。
     *
     * @param channel 支付渠道
     * @return 对应渠道适配器
     */
    public PayChannelAdapter getAdapter(PayChannel channel) {
        log.info("开始解析支付渠道适配器，支付渠道={}", channel);
        PayChannelAdapter adapter = adapters.get(channel);
        if (adapter == null) {
            log.error("未找到对应的支付渠道适配器，支付渠道={}", channel);
            throw new IllegalArgumentException("未找到支付渠道适配器: " + channel);
        }
        log.info("支付渠道适配器解析完成，支付渠道={}, 适配器实现={}",
                channel, adapter.getClass().getSimpleName());
        return adapter;
    }
}
