package org.lml.entity.dto.ai;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 *  AI-视频-文件 关联表
 * </p>
 *
 * @author lml
 * @since 2025-11-21
 */
@TableName("file_video_association")
@Data
public class FileVideoAssociation implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "association_id", type = IdType.AUTO)
    private Integer associationId;

    private Integer userId;

    private Integer fileId;

    private Integer userVideoId;

    private String creator;

    private LocalDateTime createTime;

    private Boolean isActive;

    private String fileType;

    private String lastModifiedBy;

    private LocalDateTime lastModifiedTime;
}
