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
@TableName("picture_file")
@Data
public class PictureFile implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "picture_file_id", type = IdType.AUTO)
    private Long pictureFileId;

    /**
     * 创建时间
     */
    private String createTime;

    /**
     * 创建用户id
     */
    private Long createUserId;

    /**
     * 扩展名
     */
    private String extendName;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * 文件url
     */
    private String fileUrl;

    /**
     * 修改时间
     */
    private String modifyTime;

    /**
     * 修改用户id
     */
    private Long modifyUserId;

    /**
     * 存储类型
     */
    private Integer storageType;

    /**
     * 用户id
     */
    private Long userId;

}
