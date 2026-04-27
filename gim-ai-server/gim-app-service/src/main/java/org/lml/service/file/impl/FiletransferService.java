package org.lml.service.file.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qiwenshare.common.util.MimeUtils;
import com.qiwenshare.ufop.constant.StorageTypeEnum;
import com.qiwenshare.ufop.exception.operation.DownloadException;
import com.qiwenshare.ufop.exception.operation.UploadException;
import com.qiwenshare.ufop.factory.UFOPFactory;
import com.qiwenshare.ufop.operation.delete.Deleter;
import com.qiwenshare.ufop.operation.delete.domain.DeleteFile;
import com.qiwenshare.ufop.operation.download.Downloader;
import com.qiwenshare.ufop.operation.download.domain.DownloadFile;
import com.qiwenshare.ufop.operation.preview.Previewer;
import com.qiwenshare.ufop.operation.preview.domain.PreviewFile;
import com.qiwenshare.ufop.util.UFOPUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.lml.entity.dto.file.*;
import org.lml.entity.req.DownloadFileDTO;
import org.lml.entity.req.PreviewDTO;
import org.lml.mapper.file.FileMapper;
import org.lml.mapper.file.PictureFileMapper;
import org.lml.mapper.file.UserFileMapper;
import org.lml.service.file.IFiletransferService;
import org.lml.utils.file.QiwenFile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.zip.Adler32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@Transactional(rollbackFor=Exception.class)
public class FiletransferService implements IFiletransferService {


    @Resource
    UFOPFactory ufopFactory;

    @Resource
    private UserFileMapper userFileMapper;

    @Resource
    private FileMapper fileMapper;

    @Resource
    private PictureFileMapper pictureFileMapper;

    public static Executor exec = Executors.newFixedThreadPool(20);


    private String formatChatset(String str) {
        if (str == null) {
            return "";
        }
        if (Charset.forName("ISO-8859-1").newEncoder().canEncode(str)) {
            byte[] bytes = str.getBytes(StandardCharsets.ISO_8859_1);
            return new String(bytes, Charset.forName("GBK"));
        }
        return str;
    }


    @Override
    public void downloadUserFileList(HttpServletResponse httpServletResponse, String filePath, String fileName, List<String> userFileIds) {
        String staticPath = UFOPUtils.getStaticPath();
        String tempPath = staticPath + "temp" + File.separator;
        File tempDirFile = new File(tempPath);
        if (!tempDirFile.exists()) {
            tempDirFile.mkdirs();
        }

        FileOutputStream f = null;
        try {
            f = new FileOutputStream(tempPath + fileName + ".zip");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        CheckedOutputStream csum = new CheckedOutputStream(f, new Adler32());
        ZipOutputStream zos = new ZipOutputStream(csum);
        BufferedOutputStream out = new BufferedOutputStream(zos);

        try {
            for (String userFileId : userFileIds) {
                UserFile userFile1 = userFileMapper.selectById(userFileId);
                if (userFile1.isFile()) {
                    FileBean fileBean = fileMapper.selectById(userFile1.getFileId());
                    Downloader downloader = ufopFactory.getDownloader(fileBean.getStorageType());
                    if (downloader == null) {
                        log.error("下载失败，文件存储类型不支持下载，storageType:{}", fileBean.getStorageType());
                        throw new UploadException("下载失败");
                    }
                    DownloadFile downloadFile = new DownloadFile();
                    downloadFile.setFileUrl(fileBean.getFileUrl());
                    InputStream inputStream = downloader.getInputStream(downloadFile);
                    BufferedInputStream bis = new BufferedInputStream(inputStream);
                    try {
                        QiwenFile qiwenFile = new QiwenFile(StrUtil.removePrefix(userFile1.getFilePath(), filePath), userFile1.getFileName() + "." + userFile1.getExtendName(), false);
                        zos.putNextEntry(new ZipEntry(qiwenFile.getPath()));

                        byte[] buffer = new byte[1024];
                        int i = bis.read(buffer);
                        while (i != -1) {
                            out.write(buffer, 0, i);
                            i = bis.read(buffer);
                        }
                    } catch (IOException e) {
                        log.error("" + e);
                        e.printStackTrace();
                    } finally {
                        IOUtils.closeQuietly(bis);
                        try {
                            out.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    QiwenFile qiwenFile = new QiwenFile(StrUtil.removePrefix(userFile1.getFilePath(), filePath), userFile1.getFileName(), true);
                    // 空文件夹的处理
                    zos.putNextEntry(new ZipEntry(qiwenFile.getPath() + QiwenFile.separator));
                    // 没有文件，不需要文件的copy
                    zos.closeEntry();
                }
            }

        } catch (Exception e) {
            log.error("压缩过程中出现异常:"+ e);
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        String zipPath = "";
        try {
            Downloader downloader = ufopFactory.getDownloader(StorageTypeEnum.LOCAL.getCode());
            DownloadFile downloadFile = new DownloadFile();
            downloadFile.setFileUrl("temp" + File.separator + fileName + ".zip");
            File tempFile = new File(UFOPUtils.getStaticPath() + downloadFile.getFileUrl());
            httpServletResponse.setContentLengthLong(tempFile.length());
            downloader.download(httpServletResponse, downloadFile);
            zipPath = UFOPUtils.getStaticPath() + "temp" + File.separator + fileName + ".zip";
        } catch (Exception e) {
            //org.apache.catalina.connector.ClientAbortException: java.io.IOException: Connection reset by peer
            if (e.getMessage().contains("ClientAbortException")) {
                //该异常忽略不做处理
            } else {
                log.error("下传zip文件出现异常：{}", e.getMessage());
            }

        } finally {
            File file = new File(zipPath);
            if (file.exists()) {
                file.delete();
            }
        }
    }

    @Override
    public void downloadFile(HttpServletResponse httpServletResponse, DownloadFileDTO downloadFileDTO) {
        UserFile userFile = userFileMapper.selectById(downloadFileDTO.getUserFileId());

        if (userFile.isFile()) {

            FileBean fileBean = fileMapper.selectById(userFile.getFileId());
            Downloader downloader = ufopFactory.getDownloader(fileBean.getStorageType());
            if (downloader == null) {
                log.error("下载失败，文件存储类型不支持下载，storageType:{}", fileBean.getStorageType());
                throw new DownloadException("下载失败");
            }
            DownloadFile downloadFile = new DownloadFile();

            downloadFile.setFileUrl(fileBean.getFileUrl());
            httpServletResponse.setContentLengthLong(fileBean.getFileSize());
            downloader.download(httpServletResponse, downloadFile);
        } else {

            QiwenFile qiwenFile = new QiwenFile(userFile.getFilePath(), userFile.getFileName(), true);
            List<UserFile> userFileList = userFileMapper.selectUserFileByLikeRightFilePath(qiwenFile.getPath() , userFile.getUserId());
            List<String> userFileIds = userFileList.stream().map(UserFile::getUserFileId).collect(Collectors.toList());

            downloadUserFileList(httpServletResponse, userFile.getFilePath(), userFile.getFileName(), userFileIds);
        }
    }


    @Override
    public void previewFile(HttpServletResponse httpServletResponse, PreviewDTO previewDTO) {
        UserFile userFile = userFileMapper.selectById(previewDTO.getUserFileId());
        FileBean fileBean = fileMapper.selectById(userFile.getFileId());
        Previewer previewer = ufopFactory.getPreviewer(fileBean.getStorageType());
        if (previewer == null) {
            log.error("预览失败，文件存储类型不支持预览，storageType:{}", fileBean.getStorageType());
            throw new UploadException("预览失败");
        }
        PreviewFile previewFile = new PreviewFile();
        previewFile.setFileUrl(fileBean.getFileUrl());
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


    @Override
    public void previewPictureFile(HttpServletResponse httpServletResponse, PreviewDTO previewDTO) {
        byte[] bytesUrl = Base64.getDecoder().decode(previewDTO.getUrl());
        PictureFile pictureFile = new PictureFile();
        pictureFile.setFileUrl(new String(bytesUrl));
        pictureFile = pictureFileMapper.selectOne(new QueryWrapper<>(pictureFile));
        Previewer previewer = ufopFactory.getPreviewer(pictureFile.getStorageType());
        if (previewer == null) {
            log.error("预览失败，文件存储类型不支持预览，storageType:{}", pictureFile.getStorageType());
            throw new UploadException("预览失败");
        }
        PreviewFile previewFile = new PreviewFile();
        previewFile.setFileUrl(pictureFile.getFileUrl());
//        previewFile.setFileSize(pictureFile.getFileSize());
        try {

            String mime= MimeUtils.getMime(pictureFile.getExtendName());
            httpServletResponse.setHeader("Content-Type", mime);

            String fileName = pictureFile.getFileName() + "." + pictureFile.getExtendName();
            try {
                fileName = new String(fileName.getBytes("utf-8"), "ISO-8859-1");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            httpServletResponse.addHeader("Content-Disposition", "fileName=" + fileName);// 设置文件名

            previewer.imageOriginalPreview(httpServletResponse, previewFile);
        } catch (Exception e){
            //org.apache.catalina.connector.ClientAbortException: java.io.IOException: 你的主机中的软件中止了一个已建立的连接。
            if (e.getMessage().contains("ClientAbortException")) {
                //该异常忽略不做处理
            } else {
                log.error("预览文件出现异常：{}", e.getMessage());
            }

        }
    }


    @Override
    public void deleteFile(FileBean fileBean) {
        Deleter deleter = null;

        deleter = ufopFactory.getDeleter(fileBean.getStorageType());
        DeleteFile deleteFile = new DeleteFile();
        deleteFile.setFileUrl(fileBean.getFileUrl());
        deleter.delete(deleteFile);
    }

}
