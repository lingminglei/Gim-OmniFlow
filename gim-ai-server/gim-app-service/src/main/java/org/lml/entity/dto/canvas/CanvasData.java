package org.lml.entity.dto.canvas;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * <p>
 *
 * </p>
 *
 * @author lml
 * @since 2025-12-15
 */
@TableName("canvas_data")
@Data
public class CanvasData implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键，项目唯一 ID
     */
    private Long id;

    /**
     * 所属用户 ID
     */
    private Long userId;

    /**
     * 画布名称
     */
    private String projectName;

    /**
     * 画布内容版本号
     */
    private Integer contentVersion;

    /**
     * 完整的画布状态序列化 JSON 字符串
     */
    private String canvasStateJson;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 最后更新时间
     */
    private Date updateTime;


}
