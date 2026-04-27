package org.lml.business.billing.domain.tcc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.lml.business.billing.domain.tcc.entity.BillingTccRecord;

/**
 * 积分 TCC 记录 Mapper。
 */
@Mapper
public interface BillingTccRecordMapper extends BaseMapper<BillingTccRecord> {

    /**
     * 仅允许 TRYING 状态推进到 CONFIRMED。
     *
     * @param id 记录主键
     * @param confirmRequestNo 确认请求号
     * @return 影响行数
     */
    @Update("UPDATE billing_tcc_record " +
            "SET status = 'CONFIRMED', confirm_request_no = #{confirmRequestNo}, update_time = NOW() " +
            "WHERE id = #{id} AND status = 'TRYING'")
    int confirmFromTrying(@Param("id") Long id, @Param("confirmRequestNo") String confirmRequestNo);

    /**
     * 仅允许 TRYING 状态推进到 CANCELED。
     *
     * @param id 记录主键
     * @param cancelRequestNo 取消请求号
     * @param lastErrorMsg 错误信息
     * @return 影响行数
     */
    @Update("UPDATE billing_tcc_record " +
            "SET status = 'CANCELED', cancel_request_no = #{cancelRequestNo}, last_error_msg = #{lastErrorMsg}, update_time = NOW() " +
            "WHERE id = #{id} AND status = 'TRYING'")
    int cancelFromTrying(@Param("id") Long id,
                         @Param("cancelRequestNo") String cancelRequestNo,
                         @Param("lastErrorMsg") String lastErrorMsg);
}
