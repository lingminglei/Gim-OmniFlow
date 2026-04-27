package org.lml.entity.dto.file;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 上传任务详情表
 * </p>
 *
 * @author lml
 * @since 2025-07-22
 */
@TableName("upload_task_detail")
@Data
public class UploadTaskDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "upload_task_detail_id", type = IdType.AUTO)
    private Long uploadTaskDetailId;

    /**
     * 当前分片数
     */
    private Integer chunkNumber;

    /**
     * 当前分片大小
     */
    private Long chunkSize;

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 文件名称
     */
    private String fileName;

    /**
     * 文件md5唯一标识
     */
    private String identifier;

    /**
     * 文件相对路径
     */
    private String relativePath;

    /**
     * 文件总分片数
     */
    private Integer totalChunks;

    /**
     * 文件总大小
     */
    private Long totalSize;

}
