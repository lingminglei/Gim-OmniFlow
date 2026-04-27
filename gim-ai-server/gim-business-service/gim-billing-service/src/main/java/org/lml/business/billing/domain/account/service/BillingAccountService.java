package org.lml.business.billing.domain.account.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.lml.billing.resp.BillingBalanceResp;
import org.lml.business.billing.domain.account.entity.BillingAccount;
import org.lml.business.billing.domain.account.mapper.BillingAccountMapper;
import org.lml.business.billing.exception.BillingBizException;
import org.lml.business.billing.support.BillingAccountSnapshot;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;

/**
 * 积分账户服务。
 * 负责账户初始化、余额查询以及原子加减账操作。
 */
@Slf4j
@Service
public class BillingAccountService {

    private static final long DEFAULT_DISK_LIMIT = 1024L * 1024L * 1024L;

    @Resource
    private BillingAccountMapper billingAccountMapper;

    /**
     * 查询积分余额，不存在账户时自动初始化。
     *
     * @param userId 用户 ID
     * @return 余额响应
     */
    public BillingBalanceResp queryBalance(String userId) {
        BillingAccount account = getOrCreateAccount(userId);
        BillingBalanceResp response = new BillingBalanceResp();
        response.setUserId(userId);
        response.setBalance(safeAmount(account.getBalance()));
        response.setFrozenBalance(safeAmount(account.getFrozenBalance()));
        return response;
    }

    /**
     * 获取账户，不存在则初始化一条零余额账户。
     *
     * @param userId 用户 ID
     * @return 账户信息
     */
    public BillingAccount getOrCreateAccount(String userId) {
        //根据用户ID 获取用户资产信息
        BillingAccount account = getAccount(userId);
        if (account != null) {
            //返回安全对象
            return normalizeAccount(account);
        }

        //用户资产信息不存在，则初始化一条余额为0的账号
        BillingAccount initAccount = new BillingAccount();
        initAccount.setUserId(userId);
        initAccount.setBalance(BigDecimal.ZERO);
        initAccount.setFrozenBalance(BigDecimal.ZERO);
        initAccount.setDiskUsedBytes(0L);
        initAccount.setDiskLimitBytes(DEFAULT_DISK_LIMIT);
        try {
            billingAccountMapper.insert(initAccount);
        } catch (DuplicateKeyException ex) {
            log.warn("积分账户初始化遇到并发插入，转为重新查询，userId={}", userId);
        }

        account = getAccount(userId);
        if (account == null) {
            throw new BillingBizException("初始化积分账户失败");
        }
        return normalizeAccount(account);
    }

    /**
     * 执行积分充值。
     *
     * @param userId 用户 ID
     * @param amount 充值积分
     * @return 账户快照
     */
    public BillingAccountSnapshot increaseBalance(String userId, BigDecimal amount) {
        //判断积分数据校验
        assertPositiveAmount(amount);
        //获取用户资产信息
        BillingAccount before = getOrCreateAccount(userId);
        //积分充值操作
        if (billingAccountMapper.increaseBalance(userId, amount) <= 0) {
            throw new BillingBizException("积分充值失败");
        }
        //获取最新用户资产信息
        BillingAccount after = requireAccount(userId);
        //构建返回参数
        return buildSnapshot(userId, before, after);
    }

    /**
     * TCC Try 阶段预扣减积分。
     *
     * @param userId 用户 ID
     * @param amount 扣减积分
     * @return 账户快照
     */
    public BillingAccountSnapshot tryFreeze(String userId, BigDecimal amount) {
        //校验积分数量是否合法
        assertPositiveAmount(amount);
        //获取用户资产信息
        BillingAccount before = getOrCreateAccount(userId);
        //判断积分是否够扣减
        if (safeAmount(before.getBalance()).compareTo(amount) < 0) {
            throw new BillingBizException("积分余额不足");
        }
        //数据库冻结积分预扣
        if (billingAccountMapper.tryFreeze(userId, amount) <= 0) {
            throw new BillingBizException("积分预扣减失败");
        }
        BillingAccount after = requireAccount(userId);
        return buildSnapshot(userId, before, after);
    }

    /**
     * TCC Confirm 阶段正式扣减冻结积分。
     *
     * @param userId 用户 ID
     * @param amount 扣减积分
     * @return 账户快照
     */
    public BillingAccountSnapshot confirmDebit(String userId, BigDecimal amount) {
        assertPositiveAmount(amount);
        BillingAccount before = getOrCreateAccount(userId);
        if (safeAmount(before.getFrozenBalance()).compareTo(amount) < 0) {
            throw new BillingBizException("冻结积分不足，无法确认扣减");
        }
        if (billingAccountMapper.confirmDebit(userId, amount) <= 0) {
            throw new BillingBizException("积分确认扣减失败");
        }
        BillingAccount after = requireAccount(userId);
        return buildSnapshot(userId, before, after);
    }

    /**
     * TCC Cancel 阶段释放冻结积分回可用积分。
     *
     * @param userId 用户 ID
     * @param amount 回退积分
     * @return 账户快照
     */
    public BillingAccountSnapshot cancelDebit(String userId, BigDecimal amount) {
        assertPositiveAmount(amount);
        BillingAccount before = getOrCreateAccount(userId);
        if (safeAmount(before.getFrozenBalance()).compareTo(amount) < 0) {
            throw new BillingBizException("冻结积分不足，无法取消扣减");
        }
        if (billingAccountMapper.cancelDebit(userId, amount) <= 0) {
            throw new BillingBizException("积分取消扣减失败");
        }
        BillingAccount after = requireAccount(userId);
        return buildSnapshot(userId, before, after);
    }

    private BillingAccount getAccount(String userId) {
        return billingAccountMapper.selectOne(new LambdaQueryWrapper<BillingAccount>()
                .eq(BillingAccount::getUserId, userId)
                .last("limit 1"));
    }

    private BillingAccount requireAccount(String userId) {
        BillingAccount account = getAccount(userId);
        if (account == null) {
            throw new BillingBizException("积分账户不存在");
        }
        return normalizeAccount(account);
    }

    private BillingAccountSnapshot buildSnapshot(String userId, BillingAccount before, BillingAccount after) {
        return BillingAccountSnapshot.builder()
                .userId(userId)
                .balanceBefore(safeAmount(before.getBalance()))
                .balanceAfter(safeAmount(after.getBalance()))
                .frozenBefore(safeAmount(before.getFrozenBalance()))
                .frozenAfter(safeAmount(after.getFrozenBalance()))
                .build();
    }

    private BillingAccount normalizeAccount(BillingAccount account) {
        account.setBalance(safeAmount(account.getBalance()));
        account.setFrozenBalance(safeAmount(account.getFrozenBalance()));
        return account;
    }

    private BigDecimal safeAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private void assertPositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BillingBizException("积分数量必须大于 0");
        }
    }
}
