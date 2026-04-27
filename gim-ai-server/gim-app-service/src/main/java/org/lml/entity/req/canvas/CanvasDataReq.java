package org.lml.entity.req.canvas;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import javax.validation.constraints.NotBlank;
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
@Data
public class CanvasDataReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 所属用户 ID
     */
    private Long userId;

    /**
     * 画布名称
     */
    @NotBlank(message = "画布版本名称不能为空")
    private String projectName;

    /**
     * 画布内容版本号
     */
    private Integer contentVersion;

    /**
     * 完整的画布状态序列化 JSON 字符串
     */
    @NotBlank(message = "画布版本内容不能为空")
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
