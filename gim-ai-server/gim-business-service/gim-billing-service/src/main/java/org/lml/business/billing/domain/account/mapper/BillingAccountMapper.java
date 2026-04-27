package org.lml.business.billing.domain.account.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.lml.business.billing.domain.account.entity.BillingAccount;

import java.math.BigDecimal;

/**
 * 积分账户 Mapper。
 */
@Mapper
public interface BillingAccountMapper extends BaseMapper<BillingAccount> {

    /**
     * 原子增加可用积分。
     *
     * @param userId 用户 ID
     * @param amount 增加积分
     * @return 影响行数
     */
    @Update("UPDATE user_assets " +
            "SET balance = IFNULL(balance, 0) + #{amount}, update_time = NOW() " +
            "WHERE user_id = #{userId}")
    int increaseBalance(@Param("userId") String userId, @Param("amount") BigDecimal amount);

    /**
     * 原子执行预扣减，把可用积分转入冻结积分。
     *
     * @param userId 用户 ID
     * @param amount 扣减积分
     * @return 影响行数
     */
    @Update("UPDATE user_assets " +
            "SET balance = IFNULL(balance, 0) - #{amount}, " +
            "    frozen_balance = IFNULL(frozen_balance, 0) + #{amount}, " +
            "    update_time = NOW() " +
            "WHERE user_id = #{userId} AND IFNULL(balance, 0) >= #{amount}")
    int tryFreeze(@Param("userId") String userId, @Param("amount") BigDecimal amount);

    /**
     * 原子确认扣减，从冻结积分转为真实扣减。
     *
     * @param userId 用户 ID
     * @param amount 扣减积分
     * @return 影响行数
     */
    @Update("UPDATE user_assets " +
            "SET frozen_balance = IFNULL(frozen_balance, 0) - #{amount}, update_time = NOW() " +
            "WHERE user_id = #{userId} AND IFNULL(frozen_balance, 0) >= #{amount}")
    int confirmDebit(@Param("userId") String userId, @Param("amount") BigDecimal amount);

    /**
     * 原子回退预扣减，把冻结积分释放回可用积分。
     *
     * @param userId 用户 ID
     * @param amount 回退积分
     * @return 影响行数
     */
    @Update("UPDATE user_assets " +
            "SET balance = IFNULL(balance, 0) + #{amount}, " +
            "    frozen_balance = IFNULL(frozen_balance, 0) - #{amount}, " +
            "    update_time = NOW() " +
            "WHERE user_id = #{userId} AND IFNULL(frozen_balance, 0) >= #{amount}")
    int cancelDebit(@Param("userId") String userId, @Param("amount") BigDecimal amount);
}
