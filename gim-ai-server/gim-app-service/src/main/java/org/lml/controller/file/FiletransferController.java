package org.lml.controller.file;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qiwenshare.common.anno.MyLog;
import com.qiwenshare.common.result.RestResult;
import com.qiwenshare.common.util.MimeUtils;
import com.qiwenshare.ufop.factory.UFOPFactory;
import com.qiwenshare.ufop.operation.download.Downloader;
import com.qiwenshare.ufop.operation.download.domain.DownloadFile;
import com.qiwenshare.ufop.operation.download.domain.Range;
import com.qiwenshare.ufop.operation.upload.domain.UploadFileResult;
import com.qiwenshare.ufop.util.UFOPUtils;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.lml.common.result.CommonResult;
import org.lml.entity.dto.file.FileBean;
import org.lml.entity.req.*;
import org.lml.entity.dto.file.UserFile;
import org.lml.entity.resp.UploadFileDTO;
import org.lml.service.file.IFileService;
import org.lml.service.file.IUserFileService;
import org.lml.service.file.impl.FiletransferService;
import org.lml.utils.file.FileDealComp;
import org.lml.utils.file.QiwenFile;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 该接口为文件传输接口，主要用来做文件的上传、下载和预览
 */
@Slf4j
@RestController
@RequestMapping("/filetransfer")
public class FiletransferController {


    @Resource
    IFileService fileService;

    @Resource
    UFOPFactory ufopFactory;

    @Resource
    private FileDealComp fileDealComp;

    @Resource
    private IUserFileService userFileService;


    public static final String CURRENT_MODULE = "文件传输接口";

    @Resource
    private FiletransferService filetransferService;

    /**
     * [极速上传]
     *
     * 校验文件MD5判断文件是否存在，如果存在直接上传成功并返回skipUpload=true，
     * 如果不存在返回skipUpload=false需要再次调用该接口的POST方法", tags = {"filetransfer"})
     * @param uploadFileDto
     * @return
     */
    @RequestMapping(value = "/uploadfile", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult<UploadFileVo> uploadFileSpeed(UploadFileDTO uploadFileDto) {

        log.info("[极速上传]开始上传文件; uploadFileSpeed");

        //TODO: 下载前判断用户分配内存是否充足

        UploadFileVo uploadFileVo = new UploadFileVo();
        uploadFileVo.setSkipUpload(false);
        uploadFileVo.setNeedMerge(false);


        return CommonResult.successResponse(uploadFileVo);

    }

    /**
     * 上传文件  -- 真正的上传文件接口
     * @param request
     * @param uploadFileDto
     * @return
     */
    @RequestMapping(value = "/uploadfile", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult<UploadFileVo> uploadFile(HttpServletRequest request, UploadFileDTO uploadFileDto) {

        log.info("[极速上传]开始上传文件; uploadFile");

        fileService.uploadFile(request, uploadFileDto, String.valueOf(StpUtil.getLoginId()));

        UploadFileVo uploadFileVo = new UploadFileVo();

        return CommonResult.successResponse(uploadFileVo);

    }

    /**
     * 普通上传单个文件
     * @param request
     */
    @RequestMapping(value = "/uploadfiles", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult<UploadFileVo> uploadFiles(HttpServletRequest request) {


        List<UploadFileResult> resp = fileService.uploadFile(request);

        for(UploadFileResult uploadFileResult : resp){
            System.out.println(uploadFileResult);
        }

        UploadFileVo uploadFileVo = new UploadFileVo();

        return CommonResult.successResponse(uploadFileVo);

    }

    /**
     * 下载文件
     */
    @RequestMapping(value = "/downloadfile", method = RequestMethod.GET)
    public void downloadFile(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, DownloadFileDTO downloadFileDTO) {
        Cookie[] cookieArr = httpServletRequest.getCookies();
        String token = "";
        if (cookieArr != null) {
            for (Cookie cookie : cookieArr) {
                if ("token".equals(cookie.getName())) {
                    token = cookie.getValue();
                }
            }
        }

        //TODO: 校验是否具有校验权限： 下载、预览、分享权限

        httpServletResponse.setContentType("application/force-download");// 设置强制下载不打开
        UserFile userFile = userFileService.getById(downloadFileDTO.getUserFileId());
        String fileName = "";
        if (userFile.getIsDir() == 1) {
            fileName = userFile.getFileName() + ".zip";
        } else {
            fileName = userFile.getFileName() + "." + userFile.getExtendName();

        }
        try {
            fileName = new String(fileName.getBytes("utf-8"), "ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        httpServletResponse.addHeader("Content-Disposition", "attachment;fileName=" + fileName);// 设置文件名

        filetransferService.downloadFile(httpServletResponse, downloadFileDTO);
    }

    /**
     * 批量下载文件
     */
    @RequestMapping(value = "/batchDownloadFile", method = RequestMethod.GET)
    @MyLog(operation = "批量下载文件", module = CURRENT_MODULE)
    @ResponseBody
    public void batchDownloadFile(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, BatchDownloadFileDTO batchDownloadFileDTO) {
        Cookie[] cookieArr = httpServletRequest.getCookies();
        String token = "";
        if (cookieArr != null) {
            for (Cookie cookie : cookieArr) {
                if ("token".equals(cookie.getName())) {
                    token = cookie.getValue();
                }
            }
        }
        //TODO: 校验是否具有校验权限： 下载、预览、分享权限

        String files = batchDownloadFileDTO.getUserFileIds();
        String[] userFileIdStrs = files.split(",");
        List<String> userFileIds = new ArrayList<>();
        for(String userFileId : userFileIdStrs) {
            QueryWrapper<UserFile> qw = new QueryWrapper<UserFile>();
            System.out.println("userFileId:"+userFileId);
            qw.eq("user_file_id",userFileId);
            List<UserFile> userFiles = new ArrayList<>();
            try {
                userFiles = userFileService.list(qw);
            } catch (Exception ex) {
                log.info("[批量下载文件]获取文件失败", ex);
            }

            UserFile userFile = userFiles.get(0);
            if (userFile.getIsDir() == 0) {
                userFileIds.add(userFileId);
            } else {
                QiwenFile qiwenFile = new QiwenFile(userFile.getFilePath(), userFile.getFileName(), true);
                List<UserFile> userFileList = userFileService.selectUserFileByLikeRightFilePath(qiwenFile.getPath(), userFile.getUserId());
                List<String> userFileIds1 = userFileList.stream().map(UserFile::getUserFileId).collect(Collectors.toList());
                userFileIds.add(userFile.getUserFileId());
                userFileIds.addAll(userFileIds1);
            }

        }
        UserFile userFile = userFileService.getById(userFileIdStrs[0]);
        httpServletResponse.setContentType("application/force-download");// 设置强制下载不打开
        Date date = new Date();
        String fileName = String.valueOf(date.getTime());
        httpServletResponse.addHeader("Content-Disposition", "attachment;fileName=" + fileName + ".zip");// 设置文件名
        filetransferService.downloadUserFileList(httpServletResponse, userFile.getFilePath(), fileName, userFileIds);
    }

    /**
     * 删除文件
     */
    @RequestMapping(value = "/deletefile", method = RequestMethod.POST)
    @ResponseBody
    public RestResult deleteFile(@RequestBody DeleteFileDTO deleteFileDto) {

        userFileService.deleteUserFile(deleteFileDto.getUserFileId(), String.valueOf(StpUtil.getLoginId()));
//        fileDealComp.deleteESByUserFileId(deleteFileDto.getUserFileId());

        return RestResult.success();

    }

    /**
     * 预览文件
     */
    @Operation(summary="预览文件", description="用于文件预览", tags = {"filetransfer"})
    @GetMapping("/preview")
    public void preview(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,  PreviewDTO previewDTO) throws IOException {

        if (previewDTO.getPlatform() != null && previewDTO.getPlatform() == 2) {
            filetransferService.previewPictureFile(httpServletResponse, previewDTO);
            return ;
        }
        String token = "";
        if (StringUtils.isNotEmpty(previewDTO.getToken())) {
            token = previewDTO.getToken();
        } else {
            Cookie[] cookieArr = httpServletRequest.getCookies();
            if (cookieArr != null) {
                for (Cookie cookie : cookieArr) {
                    if ("token".equals(cookie.getName())) {
                        token = cookie.getValue();
                    }
                }
            }
        }

        //TODO: 校验是否具有校验权限： 下载、预览、分享权限
        UserFile userFile = userFileService.getById(previewDTO.getUserFileId());

        String fileName = userFile.getFileName() + "." + userFile.getExtendName();
        try {
            fileName = new String(fileName.getBytes("utf-8"), "ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        httpServletResponse.addHeader("Content-Disposition", "fileName=" + fileName);// 设置文件名
        String mime = MimeUtils.getMime(userFile.getExtendName());
        httpServletResponse.setHeader("Content-Type", mime);
        if (UFOPUtils.isImageFile(userFile.getExtendName())) {
            httpServletResponse.setHeader("cache-control", "public");
        }

        FileBean fileBean = fileService.getById(userFile.getFileId());
        if (UFOPUtils.isVideoFile(userFile.getExtendName()) || "mp3".equalsIgnoreCase(userFile.getExtendName()) || "flac".equalsIgnoreCase(userFile.getExtendName())) {
            //获取从那个字节开始读取文件
            String rangeString = httpServletRequest.getHeader("Range");
            int start = 0;
            if (StringUtils.isNotBlank(rangeString)) {
                start = Integer.parseInt(rangeString.substring(rangeString.indexOf("=") + 1, rangeString.indexOf("-")));
            }

            Downloader downloader = ufopFactory.getDownloader(fileBean.getStorageType());
            DownloadFile downloadFile = new DownloadFile();
            downloadFile.setFileUrl(fileBean.getFileUrl());
            Range range = new Range();
            range.setStart(start);

            if (start + 1024 * 1024 * 1 >= fileBean.getFileSize().intValue()) {
                range.setLength(fileBean.getFileSize().intValue() - start);
            } else {
                range.setLength(1024 * 1024 * 1);
            }
            downloadFile.setRange(range);
            InputStream inputStream = downloader.getInputStream(downloadFile);

            OutputStream outputStream = httpServletResponse.getOutputStream();
            try {

                //返回码需要为206，代表只处理了部分请求，响应了部分数据

                httpServletResponse.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                // 每次请求只返回1MB的视频流

                httpServletResponse.setHeader("Accept-Ranges", "bytes");
                //设置此次相应返回的数据范围
                httpServletResponse.setHeader("Content-Range", "bytes " + start + "-" + (fileBean.getFileSize() - 1) + "/" + fileBean.getFileSize());
                IOUtils.copy(inputStream, outputStream);


            } finally {
                IOUtils.closeQuietly(inputStream);
                IOUtils.closeQuietly(outputStream);
                if (downloadFile.getOssClient() != null) {
                    downloadFile.getOssClient().shutdown();
                }
            }

        } else {
            filetransferService.previewFile(httpServletResponse, previewDTO);
        }

    }

}
