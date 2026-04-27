package org.lml.service.file.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qiwenshare.common.util.DateUtil;
import com.qiwenshare.common.util.security.SessionUtil;
import com.qiwenshare.ufop.constant.UploadFileStatusEnum;
import com.qiwenshare.ufop.exception.operation.UploadException;
import com.qiwenshare.ufop.factory.UFOPFactory;
import com.qiwenshare.ufop.operation.upload.Uploader;
import com.qiwenshare.ufop.operation.upload.domain.UploadFile;
import com.qiwenshare.ufop.operation.upload.domain.UploadFileResult;
import com.qiwenshare.ufop.util.UFOPUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.lml.entity.dto.file.*;
import org.lml.entity.req.FileDetailVO;
import org.lml.entity.req.UploadFileVo;
import org.lml.entity.resp.FileListVO;
import org.lml.entity.resp.UploadFileDTO;
import org.lml.mapper.file.*;
import org.lml.service.file.IFileService;
import org.lml.utils.file.FileDealComp;
import org.lml.utils.file.QiwenFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lml.utils.file.QiwenFile.exec;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author lml
 * @since 2025-07-21
 */
@Service
@Slf4j
public class FileServiceImpl extends ServiceImpl<FileMapper, FileBean> implements IFileService {

    @Resource
    private FileMapper fileMapper;

    @Resource
    private UserFileMapper userFileMapper;

    @Resource
    private FileDealComp fileDealComp;

    @Resource
    private UploadTaskDetailMapper uploadTaskDetailMapper;

    @Resource
    private UploadTaskMapper uploadTaskMapper;

    @Resource
    UFOPFactory ufopFactory;

    @Resource
    private MusicMapper musicMapper;

    @Resource
    private ImageMapper imageMapper;


    @Override
    public UploadFileVo uploadFileSpeed(UploadFileDTO uploadFileDTO) {
        log.info("uploadFileSpeed 方法");
        UploadFileVo uploadFileVo = new UploadFileVo();
        Map<String, Object> param = new HashMap<>();
        param.put("identifier", uploadFileDTO.getIdentifier());
        QueryWrapper<FileBean> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("identifier",uploadFileDTO.getIdentifier());
        List<FileBean> list = fileMapper.selectByMap(param);

        String filePath = uploadFileDTO.getFilePath();
        String relativePath = uploadFileDTO.getRelativePath();
        QiwenFile qiwenFile = null;
        if (relativePath.contains("/")) {
            qiwenFile = new QiwenFile(filePath, relativePath, false);
        } else {
            qiwenFile = new QiwenFile(filePath, uploadFileDTO.getFilename(), false);
        }

        if (list != null && !list.isEmpty()) {
            FileBean file = list.get(0);
            UserFile userFile = new UserFile(qiwenFile, String.valueOf(StpUtil.getLoginId()), file.getFileId());

            try {
                userFileMapper.insert(userFile);
//                fileDealComp.uploadESByUserFileId(userFile.getUserFileId());
            } catch (Exception e) {
                log.info("极速上传文件冲突重命名处理: {}", JSON.toJSONString(userFile));

            }

            if (relativePath.contains("/")) {
                QiwenFile finalQiwenFile = qiwenFile;
                exec.execute(()->{
                    fileDealComp.restoreParentFilePath(finalQiwenFile, String.valueOf(StpUtil.getLoginId()));
                });

            }

            uploadFileVo.setSkipUpload(true);
        } else {
            uploadFileVo.setSkipUpload(false);

            List<Integer> uploaded = uploadTaskDetailMapper.selectUploadedChunkNumList(uploadFileDTO.getIdentifier());
            if (uploaded != null && !uploaded.isEmpty()) {
                uploadFileVo.setUploaded(uploaded);
            } else {

                LambdaQueryWrapper<UploadTask> lambdaQueryWrapper = new LambdaQueryWrapper<>();
                lambdaQueryWrapper.eq(UploadTask::getIdentifier, uploadFileDTO.getIdentifier());
                List<UploadTask> rslist = uploadTaskMapper.selectList(lambdaQueryWrapper);
                if (rslist == null || rslist.isEmpty()) {
                    UploadTask uploadTask = new UploadTask();
                    uploadTask.setIdentifier(uploadFileDTO.getIdentifier());
                    uploadTask.setUploadTime(DateUtil.getCurrentTime());
                    uploadTask.setUploadStatus(UploadFileStatusEnum.UNCOMPLATE.getCode());
                    uploadTask.setFileName(qiwenFile.getNameNotExtend());
                    uploadTask.setFilePath(qiwenFile.getParent());
                    uploadTask.setExtendName(qiwenFile.getExtendName());
                    uploadTask.setUserId(Long.valueOf(String.valueOf(StpUtil.getLoginId())));
                    uploadTaskMapper.insert(uploadTask);
                }
            }

        }
        return uploadFileVo;
    }

    @Override
    public void uploadFile(HttpServletRequest request, UploadFileDTO uploadFileDto, String userId) {

        log.info("uploadFile 方法");

        UploadFile uploadFile = new UploadFile();
        uploadFile.setChunkNumber(uploadFileDto.getChunkNumber());
        uploadFile.setChunkSize(uploadFileDto.getChunkSize());
        uploadFile.setTotalChunks(uploadFileDto.getTotalChunks());
        uploadFile.setIdentifier(uploadFileDto.getIdentifier());
        uploadFile.setTotalSize(uploadFileDto.getTotalSize());
        uploadFile.setCurrentChunkSize(uploadFileDto.getCurrentChunkSize());

        Uploader uploader = ufopFactory.getUploader();
        if (uploader == null) {
            log.error("上传失败，请检查storageType是否配置正确");
            throw new UploadException("上传失败");
        }
        List<UploadFileResult> uploadFileResultList;
        try {
            uploadFileResultList = uploader.upload(request, uploadFile);
        } catch (Exception e) {
            log.error("上传失败，请检查UFOP连接配置是否正确");
            throw new UploadException("上传失败", e);
        }
        for (int i = 0; i < uploadFileResultList.size(); i++){
            UploadFileResult uploadFileResult = uploadFileResultList.get(i);
            String relativePath = uploadFileDto.getRelativePath();
            QiwenFile qiwenFile = null;
            if (relativePath.contains("/")) {
                qiwenFile = new QiwenFile(uploadFileDto.getFilePath(), relativePath, false);
            } else {
                qiwenFile = new QiwenFile(uploadFileDto.getFilePath(), uploadFileDto.getFilename(), false);
            }
            if (UploadFileStatusEnum.SUCCESS.equals(uploadFileResult.getStatus())){
                FileBean fileBean = new FileBean(uploadFileResult);
                fileBean.setCreateUserId(userId);

                System.out.println("fileBean===");
                System.out.println(fileBean);
                try {
                    fileMapper.insert(fileBean);
                } catch (Exception e) {
                    log.info("identifier Duplicate: {}", fileBean.getIdentifier());
                    System.out.println("捕获异常：");
                    System.out.println(e);
                    fileBean = fileMapper.selectOne(new QueryWrapper<FileBean>().lambda().eq(FileBean::getIdentifier, fileBean.getIdentifier()));
                    System.out.println(fileBean);
                }

                UserFile userFile = new UserFile(qiwenFile, userId, fileBean.getFileId());

                try {
                    userFileMapper.insert(userFile);
//                    fileDealComp.uploadESByUserFileId(userFile.getUserFileId());
                } catch (Exception e) {
                    System.out.println("W 出现异常：");
                    System.out.println(e);
                    log.warn("文件冲突重命名处理: {}");
//                    UserFile userFile1 = userFileMapper.selectOne(new QueryWrapper<UserFile>().lambda()
//                            .eq(UserFile::getUserId, userFile.getUserId())
//                            .eq(UserFile::getFilePath, userFile.getFilePath())
//                            .eq(UserFile::getFileName, userFile.getFileName())
//                            .eq(UserFile::getExtendName, userFile.getExtendName())
//                            .eq(UserFile::getDeleteFlag, userFile.getDeleteFlag())
//                            .eq(UserFile::getIsDir, userFile.getIsDir()));
//                    FileBean file1 = fileMapper.selectById(userFile1.getFileId());
//                    System.out.println("file1==============");
//                    System.out.println(file1);
//                    if (!StringUtils.equals(fileBean.getIdentifier(), file1.getIdentifier())) {
//                        log.warn("文件冲突重命名处理: {}", JSON.toJSONString(userFile1));
//                        String fileName = fileDealComp.getRepeatFileName(userFile, userFile.getFilePath());
//                        userFile.setFileName(fileName);
//                        userFileMapper.insert(userFile);
////                        fileDealComp.uploadESByUserFileId(userFile.getUserFileId());
//                    }
                }


                if (relativePath.contains("/")) {
                    QiwenFile finalQiwenFile = qiwenFile;
                    exec.execute(()->{
                        fileDealComp.restoreParentFilePath(finalQiwenFile, userId);
                    });

                }

                LambdaQueryWrapper<UploadTaskDetail> lambdaQueryWrapper = new LambdaQueryWrapper<>();
                lambdaQueryWrapper.eq(UploadTaskDetail::getIdentifier, uploadFileDto.getIdentifier());
                uploadTaskDetailMapper.delete(lambdaQueryWrapper);

                LambdaUpdateWrapper<UploadTask> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
                lambdaUpdateWrapper.set(UploadTask::getUploadStatus, UploadFileStatusEnum.SUCCESS.getCode())
                        .eq(UploadTask::getIdentifier, uploadFileDto.getIdentifier());
                uploadTaskMapper.update(null, lambdaUpdateWrapper);


                try {
                    if (UFOPUtils.isImageFile(uploadFileResult.getExtendName())) {
                        BufferedImage src = uploadFileResult.getBufferedImage();
                        Image image = new Image();
                        image.setImageWidth(src.getWidth());
                        image.setImageHeight(src.getHeight());
                        image.setFileId(fileBean.getFileId());
                        imageMapper.insert(image);
                    }
                } catch (Exception e) {
                    log.error("生成图片缩略图失败！", e);
                }

                fileDealComp.parseMusicFile(uploadFileResult.getExtendName(), uploadFileResult.getStorageType().getCode(), uploadFileResult.getFileUrl(), fileBean.getFileId());

            } else if (UploadFileStatusEnum.UNCOMPLATE.equals(uploadFileResult.getStatus())) {
                UploadTaskDetail uploadTaskDetail = new UploadTaskDetail();
                uploadTaskDetail.setFilePath(qiwenFile.getParent());
                uploadTaskDetail.setFileName(qiwenFile.getNameNotExtend());
                uploadTaskDetail.setChunkNumber(uploadFileDto.getChunkNumber());
                uploadTaskDetail.setChunkSize((long) uploadFileDto.getChunkSize());
                uploadTaskDetail.setRelativePath(uploadFileDto.getRelativePath());
                uploadTaskDetail.setTotalChunks(uploadFileDto.getTotalChunks());
                uploadTaskDetail.setTotalSize((long) uploadFileDto.getTotalSize());
                uploadTaskDetail.setIdentifier(uploadFileDto.getIdentifier());
                uploadTaskDetailMapper.insert(uploadTaskDetail);

            } else if (UploadFileStatusEnum.FAIL.equals(uploadFileResult.getStatus())) {
                LambdaQueryWrapper<UploadTaskDetail> lambdaQueryWrapper = new LambdaQueryWrapper<>();
                lambdaQueryWrapper.eq(UploadTaskDetail::getIdentifier, uploadFileDto.getIdentifier());
                uploadTaskDetailMapper.delete(lambdaQueryWrapper);

                LambdaUpdateWrapper<UploadTask> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
                lambdaUpdateWrapper.set(UploadTask::getUploadStatus, UploadFileStatusEnum.FAIL.getCode())
                        .eq(UploadTask::getIdentifier, uploadFileDto.getIdentifier());
                uploadTaskMapper.update(null, lambdaUpdateWrapper);
            }
        }

    }

    @Override
    public List<UploadFileResult> uploadFile(HttpServletRequest request) {

        Uploader uploader = ufopFactory.getUploader();
        if (uploader == null) {
            log.error("上传失败，请检查storageType是否配置正确");
            throw new UploadException("上传失败");
        }
        List<UploadFileResult> uploadFileResultList;
        try {
            uploadFileResultList = uploader.upload(request);
        } catch (Exception e) {
            log.error("上传失败，请检查UFOP连接配置是否正确");
            throw new UploadException("上传失败", e);
        }

        return  uploadFileResultList;

    }

    @Override
    public Long getFilePointCount(String fileId) {
        LambdaQueryWrapper<UserFile> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(UserFile::getFileId, fileId);
        long count = userFileMapper.selectCount(lambdaQueryWrapper);
        return count;
    }

    @Override
    public void updateFileDetail(String userFileId, String identifier, long fileSize) {
        UserFile userFile = userFileMapper.selectById(userFileId);
        String currentTime = DateUtil.getCurrentTime();
        FileBean fileBean = new FileBean();
        fileBean.setIdentifier(identifier);
        fileBean.setFileSize(fileSize);
        fileBean.setModifyTime(currentTime);
        fileBean.setModifyUserId(String.valueOf(StpUtil.getLoginId()));
        fileBean.setFileId(userFile.getFileId());
        fileMapper.updateById(fileBean);
        userFile.setUploadTime(currentTime);
        userFile.setModifyTime(currentTime);
        userFile.setModifyUserId(String.valueOf(StpUtil.getLoginId()));
        userFileMapper.updateById(userFile);
    }

    @Override
    public FileDetailVO getFileDetail(String userFileId) {
        UserFile userFile = userFileMapper.selectById(userFileId);
        FileBean fileBean = fileMapper.selectById(userFile.getFileId());
        Music music = musicMapper.selectOne(new QueryWrapper<Music>().eq("fileId", userFile.getFileId()));
        Image image = imageMapper.selectOne(new QueryWrapper<Image>().eq("fileId", userFile.getFileId()));

        if ("mp3".equalsIgnoreCase(userFile.getExtendName()) || "flac".equalsIgnoreCase(userFile.getExtendName())) {
            if (music == null) {
                fileDealComp.parseMusicFile(userFile.getExtendName(), fileBean.getStorageType(), fileBean.getFileUrl(), fileBean.getFileId());
                music = musicMapper.selectOne(new QueryWrapper<Music>().eq("fileId", userFile.getFileId()));
            }
        }

        FileDetailVO fileDetailVO = new FileDetailVO();
        BeanUtil.copyProperties(userFile, fileDetailVO);
        BeanUtil.copyProperties(fileBean, fileDetailVO);
        fileDetailVO.setMusic(music);
        fileDetailVO.setImage(image);
        return fileDetailVO;
    }


}
