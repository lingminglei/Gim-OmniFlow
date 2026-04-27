package org.lml.service.file;

import com.baomidou.mybatisplus.extension.service.IService;
import org.lml.entity.dto.file.RecoveryFile;
import org.lml.entity.dto.file.RecoveryFileListVo;

import java.util.List;

/**
 * <p>
 *  鏈嶅姟绫?
 * </p>
 *
 * @author lml
 * @since 2025-07-23
 */
public interface IRecoveryFileService extends IService<RecoveryFile> {

    List<RecoveryFileListVo> selectRecoveryFileList(String userId);

    void restorefile(String deleteBatchNum, String filePath, String sessionUserId);
}
