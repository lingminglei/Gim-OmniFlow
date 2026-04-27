package org.lml.mapper.file;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.lml.entity.dto.file.UploadTaskDetail;

import java.util.List;

/**
 * <p>
 *  Mapper 鎺ュ彛
 * </p>
 *
 * @author lml
 * @since 2025-07-22
 */
public interface UploadTaskDetailMapper extends BaseMapper<UploadTaskDetail> {
    List<Integer> selectUploadedChunkNumList(String identifier);
}
