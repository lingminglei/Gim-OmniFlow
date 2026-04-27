package org.lml.service.pay;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.lml.common.enums.pay.UserAssetsBizType;
import org.lml.entity.dto.user.UserAssets;
import org.lml.entity.dto.user.UserAssetsStream;
import org.lml.service.user.IUserAssetsService;
import org.lml.service.user.IUserAssetsStreamService;
import org.lml.thirdService.pay.entity.event.PaySuccessEvent;
import org.lml.thirdService.pay.entity.event.RechargeCreditEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.StringJoiner;

@Service
@Slf4j
/**
 * 充值资产服务。
 * 负责积分预占、回调到账和取消释放三类资产操作。
 */
public class RechargeCreditService {
    @Resource
    private IUserAssetsService userAssetsService;
    @Resource
    private IUserAssetsStreamService userAssetsStreamService;

    /**
     * 在tryPay阶段预占本次套餐对应的积分数量。
     * @param payOrderId 支付单号
     * @param creditAmount 预占积分数量
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean reserveRechargeCredits(String payOrderId, Integer creditAmount) {
        String userId = (String)StpUtil.getLoginId();

        BigDecimal delta = toAmount(creditAmount);
        //获取用户资产信息
        UserAssets assets = getOrCreateAssets(userId);
        BigDecimal beforeFrozen = defaultAmount(assets.getFrozenBalance());
        assets.setFrozenBalance(beforeFrozen.add(delta));
        if (!userAssetsService.updateById(assets)) {
            throw new IllegalStateException("预占充值积分失败");
        }
        log.info("充值积分预占完成，用户ID={}, 支付单号={}, 预占前冻结积分={}, 预占后冻结积分={}",
                userId, payOrderId, beforeFrozen, assets.getFrozenBalance());

        return true;
    }

    /**
     * 在支付取消或失败时释放预占积分。
     *
     * @param userId 用户ID
     * @param payOrderId 支付单号
     * @param creditAmount 需要释放的积分数量
     */
    @Transactional(rollbackFor = Exception.class)
    public void releaseRechargeCredits(String userId, String payOrderId, Integer creditAmount) {
        BigDecimal delta = toAmount(creditAmount);
        UserAssets assets = getOrCreateAssets(userId);
        BigDecimal beforeFrozen = defaultAmount(assets.getFrozenBalance());
        if (beforeFrozen.compareTo(delta) < 0) {
            throw new IllegalStateException("冻结积分不足，无法释放预占积分");
        }
        assets.setFrozenBalance(beforeFrozen.subtract(delta));
        if (!userAssetsService.updateById(assets)) {
            throw new IllegalStateException("释放预占充值积分失败");
        }
        log.info("充值积分释放完成，用户ID={}, 支付单号={}, 释放前冻结积分={}, 释放后冻结积分={}",
                userId, payOrderId, beforeFrozen, assets.getFrozenBalance());
    }

    /**
     * 获取用户资产账户；如果用户还没有资产账户，则先初始化一条记录。
     *
     * @param userId 用户ID
     * @return 用户资产
     */
    private UserAssets getOrCreateAssets(String userId) {
        QueryWrapper<UserAssets> assetsQuery = new QueryWrapper<>();
        assetsQuery.eq("user_id", userId);
        UserAssets assets = userAssetsService.getOne(assetsQuery);
        if (assets != null) {
            return assets;
        }

        log.info("未查询到用户资产，开始初始化资产账户，用户ID={}", userId);
        UserAssets initAssets = new UserAssets();
        initAssets.setUserId(userId);
        initAssets.setBalance(BigDecimal.ZERO);
        initAssets.setFrozenBalance(BigDecimal.ZERO);
        initAssets.setDiskLimitBytes(1024L * 1024L * 1024L);
        initAssets.setDiskUsedBytes(0L);
        initAssets.setDeleted(0);
        userAssetsService.save(initAssets);
        return initAssets;
    }

    /**
     * 为到账流水生成稳定的数据摘要，便于审计和防篡改校验。
     *
     * @param stream 到账流水
     * @return 数据摘要
     */
    private String buildDataHash(UserAssetsStream stream) {
        String raw = new StringJoiner("|")
                .add(defaultString(stream.getUserId()))
                .add(stream.getAmount() == null ? "" : stream.getAmount().toPlainString())
                .add(stream.getBizType() == null ? "" : stream.getBizType().name())
                .add(defaultString(stream.getBizNo()))
                .add(defaultString(stream.getTransactionId()))
                .add(defaultString(stream.getPayOrderId()))
                .add(defaultString(stream.getPackageCode()))
                .add(stream.getChannel() == null ? "" : stream.getChannel().name())
                .add(stream.getBalanceBefore() == null ? "" : stream.getBalanceBefore().toPlainString())
                .add(stream.getBalanceAfter() == null ? "" : stream.getBalanceAfter().toPlainString())
                .add(defaultString(stream.getRemark()))
                .toString();
        return DigestUtils.md5Hex(raw);
    }

    /**
     * 把积分数量转换成BigDecimal。
     *
     * @param creditAmount 积分数量
     * @return BigDecimal金额
     */
    private BigDecimal toAmount(Integer creditAmount) {
        if (creditAmount == null || creditAmount <= 0) {
            throw new IllegalArgumentException("积分数量必须大于0");
        }
        return BigDecimal.valueOf(creditAmount.longValue());
    }

    /**
     * 把空金额统一转换成0。
     *
     * @param value 原始金额
     * @return 非空金额
     */
    private BigDecimal defaultAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 把空字符串统一转换为空串，避免摘要拼接出现null文本。
     *
     * @param value 原始值
     * @return 非null字符串
     */
    private String defaultString(String value) {
        return value == null ? "" : value;
    }
}
