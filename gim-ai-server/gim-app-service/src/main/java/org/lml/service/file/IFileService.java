package org.lml.service.file;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.qiwenshare.ufop.operation.upload.domain.UploadFileResult;
import org.lml.entity.dto.file.FileBean;
import org.lml.entity.req.FileDetailVO;
import org.lml.entity.req.UploadFileVo;
import org.lml.entity.resp.FileListVO;
import org.lml.entity.resp.UploadFileDTO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author lml
 * @since 2025-07-21
 */
public interface IFileService extends IService<FileBean> {

    UploadFileVo uploadFileSpeed(UploadFileDTO uploadFileDTO);

    void uploadFile(HttpServletRequest request, UploadFileDTO UploadFileDto, String userId);

    /**
     * 普通上传单个文件
     * @param request
     * @return
     */
    List<UploadFileResult> uploadFile(HttpServletRequest request);

    Long getFilePointCount(String fileId);

    void updateFileDetail(String userFileId, String identifier, long fileSize);

    FileDetailVO getFileDetail(String userFileId);
}
