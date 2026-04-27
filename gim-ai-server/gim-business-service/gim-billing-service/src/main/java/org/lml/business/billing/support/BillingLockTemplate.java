package org.lml.business.billing.support;

import org.lml.business.billing.exception.BillingBizException;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * 用户账户分布式锁模板。
 */
@Component
public class BillingLockTemplate {

    private static final String ACCOUNT_LOCK_PREFIX = "billing:account:";

    @Resource
    private RedissonClient redissonClient;

    /**
     * 在用户级分布式锁内执行账户操作。
     *
     * @param userId 用户 ID
     * @param action 具体操作
     * @return 执行结果
     */
    public <T> T executeWithUserLock(String userId, Callable<T> action) {
        RLock lock = redissonClient.getLock(ACCOUNT_LOCK_PREFIX + userId);
        boolean locked = false;
        try {
            locked = lock.tryLock(5, 15, TimeUnit.SECONDS);
            if (!locked) {
                throw new BillingBizException("获取积分账户锁失败，请稍后重试");
            }
            return action.call();
        } catch (BillingBizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BillingBizException("积分账户处理失败: " + ex.getMessage());
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
