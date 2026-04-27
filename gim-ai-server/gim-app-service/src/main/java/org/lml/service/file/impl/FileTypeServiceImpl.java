package org.lml.service.file.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.lml.entity.dto.file.FileType;
import org.lml.mapper.file.FileTypeMapper;
import org.lml.service.file.IFileTypeService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 文件类型表 (filetype)	作用：	定义文件的分类，如图片、文档、视频等。	重要字段：	fileTypeId：文件类型ID，主键，唯一标识一个文件类型。	fileTypeName：文件类型名称，如“图片”、“文档”等。 服务实现类
 * </p>
 *
 * @author lml
 * @since 2025-07-21
 */
@Service
public class FileTypeServiceImpl extends ServiceImpl<FileTypeMapper, FileType> implements IFileTypeService {

}
