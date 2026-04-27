package org.lml.entity.dto.file;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 *
 * </p>
 *
 * @author lml
 * @since 2025-07-23
 */
@Data
@TableName("recovery_file")
public class RecoveryFile implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "recovery_file_id", type = IdType.AUTO)
    private Long recoveryFileId;

    /**
     * 删除批次号
     */
    private String deleteBatchNum;

    /**
     * 删除时间
     */
    private String deleteTime;

    /**
     * 用户文件id
     */
    private String userFileId;

}
