package org.lml.service.file.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.lml.entity.dto.file.UploadTask;
import org.lml.mapper.file.UploadTaskMapper;
import org.lml.service.file.IUploadTaskService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author lml
 * @since 2025-07-22
 */
@Service
public class UploadTaskServiceImpl extends ServiceImpl<UploadTaskMapper, UploadTask> implements IUploadTaskService {

}
