package org.lml.entity.dto.file;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 文件类型表 (filetype)
 *
 * 作用：	定义文件的分类，如图片、文档、视频等。	重要字段：	fileTypeId：文件类型ID，主键，唯一标识一个文件类型。	fileTypeName：文件类型名称，如“图片”、“文档”等。
 * </p>
 *
 * @author lml
 * @since 2025-07-21
 */
@TableName("file_type")
@Data
public class FileType implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "file_id", type = IdType.ASSIGN_ID)
    private Integer fileTypeId;

    /**
     * 文件类型名
     */
    private String fileTypeName;

    /**
     * 次序
     */
    private Integer orderNum;

}
