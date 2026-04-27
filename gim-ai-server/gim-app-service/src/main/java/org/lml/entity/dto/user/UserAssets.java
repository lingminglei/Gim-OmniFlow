package org.lml.entity.dto.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.lml.common.dto.BaseEntity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * <p>
 * 用户资产表
 * </p>
 *
 * @author lml
 * @since 2026-03-12
 */
@TableName("user_assets")
@Data
public class UserAssets extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID (唯一索引关联用户表)
     */
    private String userId;

    /**
     * 可用积分余额
     */
    private BigDecimal balance;

    /**
     * 冻结积分(如视频生成预扣)
     */
    private BigDecimal frozenBalance;

    /**
     * 网盘已用空间(字节)
     */
    private Long diskUsedBytes;

    /**
     * 网盘总容量(默认1G)
     */
    private Long diskLimitBytes;

}
