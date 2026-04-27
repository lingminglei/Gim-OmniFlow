package org.lml.service.file;


import org.lml.entity.req.DownloadFileDTO;
import org.lml.entity.dto.file.FileBean;
import org.lml.entity.req.PreviewDTO;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

public interface IFiletransferService {

    /**
     * 批量下载文件
     * @param httpServletResponse
     * @param filePath
     * @param fileName
     * @param userFileIds
     */
    void downloadUserFileList(HttpServletResponse httpServletResponse, String filePath, String fileName, List<String> userFileIds);

    /**
     * 下载文件
     * @param httpServletResponse
     * @param downloadFileDTO
     */
    void downloadFile(HttpServletResponse httpServletResponse, DownloadFileDTO downloadFileDTO);

    /**
     * 预览文件
     * @param httpServletResponse
     * @param previewDTO
     */
    void previewFile(HttpServletResponse httpServletResponse, PreviewDTO previewDTO);
    /**
     * 预览图片文件
     * @param httpServletResponse
     * @param previewDTO
     */
    void previewPictureFile(HttpServletResponse httpServletResponse, PreviewDTO previewDTO);
    /**
     * 删除文件
     * @param fileBean
     */
    void deleteFile(FileBean fileBean);
}
