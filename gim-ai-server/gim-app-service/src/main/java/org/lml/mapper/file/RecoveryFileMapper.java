package org.lml.mapper.file;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.lml.entity.dto.file.RecoveryFile;
import org.lml.entity.dto.file.RecoveryFileListVo;

import java.util.List;

/**
 * <p>
 *  Mapper 鎺ュ彛
 * </p>
 *
 * @author lml
 * @since 2025-07-23
 */
public interface RecoveryFileMapper extends BaseMapper<RecoveryFile> {
    List<RecoveryFileListVo> selectRecoveryFileList(@Param("userId") String userId);
}
