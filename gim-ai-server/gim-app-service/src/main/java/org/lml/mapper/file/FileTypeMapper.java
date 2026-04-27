package org.lml.mapper.file;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.lml.entity.dto.file.FileType;

/**
 * <p>
 * 文件类型表 (filetype)	作用：	定义文件的分类，如图片、文档、视频等。	重要字段：	fileTypeId：文件类型ID，主键，唯一标识一个文件类型。	fileTypeName：文件类型名称，如“图片”、“文档”等。 Mapper 接口
 * </p>
 *
 * @author lml
 * @since 2025-07-21
 */
public interface FileTypeMapper extends BaseMapper<FileType> {

}
