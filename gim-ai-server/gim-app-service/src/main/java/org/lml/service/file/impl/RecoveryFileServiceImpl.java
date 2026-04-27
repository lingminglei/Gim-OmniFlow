package org.lml.service.file.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.lml.entity.dto.file.RecoveryFile;
import org.lml.entity.dto.file.RecoveryFileListVo;
import org.lml.entity.dto.file.UserFile;
import org.lml.mapper.file.RecoveryFileMapper;
import org.lml.mapper.file.UserFileMapper;
import org.lml.service.file.IRecoveryFileService;
import org.lml.utils.file.FileDealComp;
import org.lml.utils.file.QiwenFile;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author lml
 * @since 2025-07-23
 */
@Service
public class RecoveryFileServiceImpl extends ServiceImpl<RecoveryFileMapper, RecoveryFile> implements IRecoveryFileService {

    @Resource
    private RecoveryFileMapper recoveryFileMapper;

    @Resource
    private UserFileMapper userFileMapper;

    @Resource
    private FileDealComp fileDealComp;

    @Override
    public List<RecoveryFileListVo> selectRecoveryFileList(String userId) {
        return recoveryFileMapper.selectRecoveryFileList(userId);
    }

    @Override
    public void restorefile(String deleteBatchNum, String filePath, String sessionUserId) {

        List<UserFile> restoreUserFileList = userFileMapper.selectList(new QueryWrapper<UserFile>().lambda().eq(UserFile::getDeleteBatchNum, deleteBatchNum));
        for (UserFile restoreUserFile : restoreUserFileList) {
            restoreUserFile.setDeleteFlag(0);
            restoreUserFile.setDeleteBatchNum(deleteBatchNum);
            String fileName = fileDealComp.getRepeatFileName(restoreUserFile, restoreUserFile.getFilePath());
            if (restoreUserFile.isDirectory()) {
                if (!StringUtils.equals(fileName, restoreUserFile.getFileName())) {
                    userFileMapper.deleteById(restoreUserFile);
                } else {
                    userFileMapper.updateById(restoreUserFile);
                }
            } else if (restoreUserFile.isFile()) {
                restoreUserFile.setFileName(fileName);
                userFileMapper.updateById(restoreUserFile);
            }
        }

        QiwenFile qiwenFile = new QiwenFile(filePath, true);
        fileDealComp.restoreParentFilePath(qiwenFile, sessionUserId);

        LambdaQueryWrapper<RecoveryFile> recoveryFileServiceLambdaQueryWrapper = new LambdaQueryWrapper<>();
        recoveryFileServiceLambdaQueryWrapper.eq(RecoveryFile::getDeleteBatchNum, deleteBatchNum);
        recoveryFileMapper.delete(recoveryFileServiceLambdaQueryWrapper);
    }
}
