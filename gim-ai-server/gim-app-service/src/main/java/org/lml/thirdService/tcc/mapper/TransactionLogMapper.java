package org.lml.thirdService.tcc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.lml.thirdService.tcc.entity.TransactionLog;

/**
 * @author Hollis
 * 事务日志
 */
@Mapper
public interface TransactionLogMapper extends BaseMapper<TransactionLog> {

    /**
     * 按事务维度查询最新一条事务日志，兼容 deleted 为空的历史数据。
     *
     * @param transactionId 事务号
     * @param businessScene 业务场景
     * @param businessModule 业务模块
     * @return 最新事务日志
     */
    @Select("SELECT * FROM transaction_log " +
            "WHERE transaction_id = #{transactionId} " +
            "AND business_scene = #{businessScene} " +
            "AND business_module = #{businessModule} " +
            "ORDER BY id DESC LIMIT 1")
    TransactionLog selectLatestByBiz(@Param("transactionId") String transactionId,
                                     @Param("businessScene") String businessScene,
                                     @Param("businessModule") String businessModule);

    /**
     * 将事务日志从 TRY 推进到 CONFIRM，只允许合法状态迁移。
     *
     * @param id 事务日志主键
     * @return 受影响行数
     */
    @Update("UPDATE transaction_log " +
            "SET state = 'CONFIRM', update_time = NOW(), deleted = IFNULL(deleted, 0) " +
            "WHERE id = #{id} AND state = 'TRY'")
    int confirmFromTry(@Param("id") Long id);

    /**
     * 将事务日志从 TRY 推进到 CANCEL，只允许合法状态迁移。
     *
     * @param id 事务日志主键
     * @param cancelType cancel类型
     * @return 受影响行数
     */
    @Update("UPDATE transaction_log " +
            "SET state = 'CANCEL', cancel_type = #{cancelType}, update_time = NOW(), deleted = IFNULL(deleted, 0) " +
            "WHERE id = #{id} AND state = 'TRY'")
    int cancelFromTry(@Param("id") Long id, @Param("cancelType") String cancelType);

    /**
     * 将历史空删除标记修正为未删除，避免后续查询被逻辑删除条件过滤。
     *
     * @param id 事务日志主键
     * @return 受影响行数
     */
    @Update("UPDATE transaction_log SET deleted = 0, update_time = NOW() WHERE id = #{id} AND deleted IS NULL")
    int normalizeDeleted(@Param("id") Long id);

}
