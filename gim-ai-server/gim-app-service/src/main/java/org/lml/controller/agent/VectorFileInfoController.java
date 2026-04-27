package org.lml.controller.agent;

import com.qiwenshare.common.util.MimeUtils;
import com.qiwenshare.ufop.operation.download.Downloader;
import com.qiwenshare.ufop.operation.download.domain.DownloadFile;
import com.qiwenshare.ufop.operation.download.domain.Range;
import com.qiwenshare.ufop.util.UFOPUtils;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.lml.entity.dto.agent.VectorFileInfo;
import org.lml.entity.dto.file.FileBean;
import org.lml.entity.dto.file.UserFile;
import org.lml.entity.req.PreviewDTO;
import org.lml.service.agent.IVectorFileInfoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

/**
 * <p>
 * Agent-向量文件信息表 前端控制器
 * </p>
 *
 * @author lml
 * @since 2025-07-28
 */
@RestController
@RequestMapping("/vectorFileInfo")
@Slf4j
public class VectorFileInfoController {

    @Resource
    private IVectorFileInfoService iVectorFileInfoService;

    /**
     * 预览文件
     */
    @Operation(summary="预览文件", description="用于文件预览", tags = {"filetransfer"})
    @GetMapping("/preview")
    public void preview(HttpServletRequest httpServletRequest,
                        HttpServletResponse httpServletResponse,
                        PreviewDTO previewDTO) throws IOException {

        VectorFileInfo vectorFileInfo = iVectorFileInfoService.getById(previewDTO.getId());

        String fileName = vectorFileInfo.getFileName() + "." + vectorFileInfo.getFileType();
        try {
            fileName = new String(fileName.getBytes("utf-8"), "ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        httpServletResponse.addHeader("Content-Disposition", "fileName=" + fileName);// 设置文件名
        String mime = MimeUtils.getMime(vectorFileInfo.getFileType());
        httpServletResponse.setHeader("Content-Type", mime);
        if (UFOPUtils.isImageFile(vectorFileInfo.getFileType())) {
            httpServletResponse.setHeader("cache-control", "public");
        }


        iVectorFileInfoService.previewFile(httpServletResponse, previewDTO);

    }


}
