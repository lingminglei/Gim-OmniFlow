package org.lml.entity.dto.ai;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.bytedeco.libfreenect._freenect_context;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * <p>
 *  AI-视频分片信息
 * </p>
 *
 * @author lml
 * @since 2025-11-21
 */
@TableName("segment_info")
@Data
public class SegmentInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableField(exist = false)
    private String id;

    @TableId(value = "segment_id", type = IdType.AUTO)
    private Integer segmentId;

    /**
     * 分片标题
     */
    private String segmentTitle;

    /**
     * 分片剧情详细描述
     */
    private String segmentDescription;

    /**
     * 分片关键字
     */
    private String segmentKeywords;

    /**
     * 分片风格
     */
    private String shardingStyle;
    /**
     * 分片构图
     */
    private String fragmentedComposition;
    /**
     * 分片光影
     */
    private String fragmentedLightAndShadow;
    /**
     * 分片运镜
     */
    private String fragmentedCameraMovement;

    /**
     * 分片镜头文生图提示词
     */
    private String fragmentedShotDescription;

    /**
     * 分片开始时间
     */
    private String beginTime;
    /**
     * 分片结束时间
     */
    private String endTime;



    private String creator;

    private Date createTime;

    private Boolean isActive;

    private Integer userVideoId;

    private String lastModifiedBy;

    private Date lastModifiedTime;

}
