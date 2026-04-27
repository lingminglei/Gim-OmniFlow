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
 * @since 2025-07-22
 */
@TableName("upload_task")
@Data
public class UploadTask implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "upload_task_id", type = IdType.AUTO)
    private Long uploadTaskId;

    /**
     * 扩展名
     */
    private String extendName;

    /**
     * 文件名称
     */
    private String fileName;

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * md5唯一标识
     */
    private String identifier;

    /**
     * 上传状态(1-成功,0-失败或未完成)
     */
    private Integer uploadStatus;

    /**
     * 上传时间
     */
    private String uploadTime;

    /**
     * 用户id
     */
    private Long userId;
}
