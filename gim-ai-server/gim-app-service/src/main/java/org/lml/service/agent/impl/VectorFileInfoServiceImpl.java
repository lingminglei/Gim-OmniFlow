package org.lml.service.agent.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qiniu.storage.model.StorageType;
import com.qiwenshare.ufop.exception.operation.UploadException;
import com.qiwenshare.ufop.factory.UFOPFactory;
import com.qiwenshare.ufop.operation.preview.Previewer;
import com.qiwenshare.ufop.operation.preview.domain.PreviewFile;
import lombok.extern.slf4j.Slf4j;
import org.lml.entity.dto.agent.VectorFileInfo;
import org.lml.entity.dto.file.FileBean;
import org.lml.entity.dto.file.UserFile;
import org.lml.entity.req.PreviewDTO;
import org.lml.mapper.agent.VectorFileInfoMapper;
import org.lml.service.agent.IVectorFileInfoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>
 * Agent-向量文件信息表 服务实现类
 * </p>
 *
 * @author lml
 * @since 2025-07-28
 */
@Service
@Slf4j
public class VectorFileInfoServiceImpl extends ServiceImpl<VectorFileInfoMapper, VectorFileInfo> implements IVectorFileInfoService {

    @Resource
    private VectorFileInfoMapper vectorFileInfoMapper;

    @Resource
    UFOPFactory ufopFactory;

    @Value("${ufop.storage-type}")
    private Integer storageType;

    @Override
    public void previewFile(HttpServletResponse httpServletResponse, PreviewDTO previewDTO) {
        VectorFileInfo userFile = vectorFileInfoMapper.selectById(previewDTO.getId());
        Previewer previewer = ufopFactory.getPreviewer(storageType);
        if (previewer == null) {
            log.info("预览失败，文件存储类型不支持预览，storageType:{}",storageType);
            throw new UploadException("预览失败");
        }
        PreviewFile previewFile = new PreviewFile();
        previewFile.setFileUrl(userFile.getFilePath());
        try {
            if ("true".equals(previewDTO.getIsMin())) {
                previewer.imageThumbnailPreview(httpServletResponse, previewFile);
            } else {
                previewer.imageOriginalPreview(httpServletResponse, previewFile);
            }
        } catch (Exception e){
            //org.apache.catalina.connector.ClientAbortException: java.io.IOException: 你的主机中的软件中止了一个已建立的连接。
            if (e.getMessage().contains("ClientAbortException")) {
                //该异常忽略不做处理
            } else {
                log.error("预览文件出现异常：{}", e.getMessage());
            }

        }

    }
}
