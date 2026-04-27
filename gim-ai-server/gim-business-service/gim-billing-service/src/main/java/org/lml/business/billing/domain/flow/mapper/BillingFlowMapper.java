package org.lml.business.billing.domain.flow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.lml.business.billing.domain.flow.entity.BillingFlow;

/**
 * 积分流水 Mapper。
 */
@Mapper
public interface BillingFlowMapper extends BaseMapper<BillingFlow> {
}
