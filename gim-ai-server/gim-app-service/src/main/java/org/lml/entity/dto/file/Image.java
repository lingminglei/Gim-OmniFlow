package org.lml.entity.dto.file;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
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
@Data
public class Image implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "imageId", type = IdType.AUTO)
    private Long imageId;

    /**
     * 文件id
     */
    private String fileId;

    /**
     * 图像的高
     */
    private Integer imageHeight;

    /**
     * 图像的宽
     */
    private Integer imageWidth;
}
