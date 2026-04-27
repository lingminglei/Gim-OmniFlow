package org.lml.entity.dto.ai;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * <p>
 *  AI-视频 Dto
 * </p>
 *
 * @author lml
 * @since 2025-11-21
 */
@TableName("user_video")
@Data
public class UserVideo implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "video_id", type = IdType.AUTO)
    private Integer videoId;

    /**
     * taskId
     */
    private String videoTitle;

    private String videoDescription;

    private String videoStyle;

    private String videoLength;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 视频预览地址
     */
    private String creator;

    private Date createTime;

    /**
     * 操作信息
     */
    private String remarks;

    /**
     * 操作失败成功
     */
    private Boolean isActive;

    /**
     * 视频文件地址
     */
    private String lastModifiedBy;

    private Date lastModifiedTime;
}
