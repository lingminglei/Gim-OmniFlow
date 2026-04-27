package org.lml.controller.file;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qiwenshare.common.exception.QiwenException;
import com.qiwenshare.common.util.DateUtil;
import com.qiwenshare.common.util.security.SessionUtil;
import com.qiwenshare.ufop.factory.UFOPFactory;
import com.qiwenshare.ufop.operation.copy.Copier;
import com.qiwenshare.ufop.operation.copy.domain.CopyFile;
import com.qiwenshare.ufop.operation.upload.domain.UploadFileResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.util.StringUtil;
import org.lml.common.result.CommonResult;
import org.lml.entity.dto.file.BatchDeleteFileDTO;
import org.lml.entity.dto.file.FileBean;
import org.lml.entity.dto.file.UserFile;
import org.lml.entity.req.*;
import org.lml.entity.resp.FileListVO;
import org.lml.service.file.IFileService;
import org.lml.service.file.IUserFileService;
import org.lml.utils.file.FileDealComp;
import org.lml.utils.file.QiwenFile;
import org.lml.utils.file.QiwenFileUtil;
import org.lml.utils.file.TreeNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author lml
 * @since 2025-07-21
 */
@RestController
@RequestMapping("/file")
@Slf4j
public class FileController {

    @Resource
    private IUserFileService userFileService;

    @Resource
    private IFileService fileService;

    @Resource
    private FileDealComp fileDealComp;

    @Resource
    UFOPFactory ufopFactory;

    @Value("${ufop.storage-type}")
    private Integer storageType;

    public static Executor executor = Executors.newFixedThreadPool(20);

    /**
     * 获取文件列表: 用来做前台列表展示
     */
    @RequestMapping(value = "/getfilelist", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult<IPage<FileListVO>> getFileList(
            @Parameter(description = "文件类型", required = true) String fileType,
            @Parameter(description = "文件路径", required = true) String filePath,
            @Parameter(description = "当前页", required = true) long currentPage,
            @Parameter(description = "页面数量", required = true) long pageCount){

        String userId = String.valueOf(StpUtil.getLoginId());
        log.info("登录用户ID={}",userId);

        if ("0".equals(0)) {
            IPage<FileListVO> fileList = userFileService.userFileList(userId, filePath, currentPage, pageCount);
            return CommonResult.successResponse(fileList);
        } else {
            if(fileType.equals("0")){
                IPage<FileListVO> fileList = userFileService.userFileList(userId, filePath, currentPage, pageCount);
                return CommonResult.successResponse(fileList);
            }else{
                IPage<FileListVO> fileList = userFileService.getFileByFileType(Integer.valueOf(fileType), currentPage, pageCount, String.valueOf(StpUtil.getLoginId()));
                return CommonResult.successResponse(fileList);
            }
        }
    }

    /**
     * 删除文件: 可以删除文件或者目录
     * @param deleteFileDto
     * @return
     */
    @RequestMapping(value = "/deletefile", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult deleteFile(@RequestBody DeleteFileDTO deleteFileDto) {

        userFileService.deleteUserFile(deleteFileDto.getUserFileId(), String.valueOf(StpUtil.getLoginId()));
//        fileDealComp.deleteESByUserFileId(deleteFileDto.getUserFileId());

        return CommonResult.successResponse("操作成功！");
    }

    /**
     * 修改文件: 支持普通文本类文件的修改
     * @param updateFileDTO
     * @return
     */
    @RequestMapping(value = "/update", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult<String> updateFile(@RequestBody UpdateFileDTO updateFileDTO) {
//        JwtUser sessionUserBean =  SessionUtil.getSession();
        UserFile userFile = userFileService.getById(updateFileDTO.getUserFileId());
        FileBean fileBean = fileService.getById(userFile.getFileId());
        Long pointCount = fileService.getFilePointCount(userFile.getFileId());
        String fileUrl = fileBean.getFileUrl();
        if (pointCount > 1) {
            fileUrl = fileDealComp.copyFile(fileBean, userFile);
        }
        String content = updateFileDTO.getFileContent();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(content.getBytes());
        try {
            int fileSize = byteArrayInputStream.available();
            fileDealComp.saveFileInputStream(fileBean.getStorageType(), fileUrl, byteArrayInputStream);

            String md5Str = fileDealComp.getIdentifierByFile(fileUrl, fileBean.getStorageType());

            fileService.updateFileDetail(userFile.getUserFileId(), md5Str, fileSize);

        } catch (Exception e) {
            System.out.println("begin====");
            System.out.println(e);
            System.out.println("end====");
            throw new QiwenException(999999, "修改文件异常");
        } finally {
            try {
                byteArrayInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return CommonResult.successResponse("修改文件成功");
    }

    /**
     * 查询文件详情
     */
    @RequestMapping(value = "/detail", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult<FileDetailVO> queryFileDetail(
            @Parameter(description = "用户文件Id", required = true) String userFileId){
        FileDetailVO vo = fileService.getFileDetail(userFileId);
        return CommonResult.successResponse(vo);
    }

    /**
     * 文件重命名
     */
    @RequestMapping(value = "/renamefile", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult<String> renameFile(@RequestBody RenameFileDTO renameFileDto) {

//        JwtUser sessionUserBean =  SessionUtil.getSession();
        UserFile userFile = userFileService.getById(renameFileDto.getUserFileId());

        List<UserFile> userFiles = userFileService.selectUserFileByNameAndPath(renameFileDto.getFileName(), userFile.getFilePath(), String.valueOf(StpUtil.getLoginId()));
        if (userFiles != null && !userFiles.isEmpty()) {
            return CommonResult.errorResponse("同名文件已存在");
        }

        LambdaUpdateWrapper<UserFile> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        lambdaUpdateWrapper.set(UserFile::getFileName, renameFileDto.getFileName())
                .set(UserFile::getUploadTime, DateUtil.getCurrentTime())
                .eq(UserFile::getUserFileId, renameFileDto.getUserFileId());
        userFileService.update(lambdaUpdateWrapper);
        if (1 == userFile.getIsDir()) {
            List<UserFile> list = userFileService.selectUserFileByLikeRightFilePath(new QiwenFile(userFile.getFilePath(), userFile.getFileName(), true).getPath(), String.valueOf(StpUtil.getLoginId()));

            for (UserFile newUserFile : list) {
                String escapedPattern = Pattern.quote(new QiwenFile(userFile.getFilePath(), userFile.getFileName(), userFile.getIsDir() == 1).getPath());
                newUserFile.setFilePath(newUserFile.getFilePath().replaceFirst(escapedPattern,
                        new QiwenFile(userFile.getFilePath(), renameFileDto.getFileName(), userFile.getIsDir() == 1).getPath()));
                userFileService.updateById(newUserFile);
            }
        }
//        fileDealComp.uploadESByUserFileId(renameFileDto.getUserFileId());
        return CommonResult.successResponse("操作成功");
    }


    /**
     * 创建文件夹： "目录(文件夹)的创建
     */
    @RequestMapping(value = "/createFold", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult<String> createFold(@Valid @RequestBody CreateFoldDTO createFoldDto) {

        String userId = String.valueOf(StpUtil.getLoginId());
        String filePath = createFoldDto.getFilePath();

        boolean isDirExist = fileDealComp.isDirExist(createFoldDto.getFileName(), createFoldDto.getFilePath(), userId);

        if (isDirExist) {
            return CommonResult.errorResponse("同名文件夹已存在");
        }

        UserFile userFile = QiwenFileUtil.getQiwenDir(userId, filePath, createFoldDto.getFileName());

        userFileService.save(userFile);
//        fileDealComp.uploadESByUserFileId(userFile.getUserFileId());
        return CommonResult.successResponse("操作成功");
    }

    /**
     * 获取文件树: 文件移动的时候需要用到该接口，用来展示目录树
     * @return
     */
    @RequestMapping(value = "/getfiletree", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult<TreeNode> getFileTree() {
        List<UserFile> userFileList = userFileService.selectFilePathTreeByUserId(String.valueOf(StpUtil.getLoginId()));
        TreeNode resultTreeNode = new TreeNode();
        resultTreeNode.setLabel(QiwenFile.separator);
        resultTreeNode.setId(0L);
        long id = 1;
        for (int i = 0; i < userFileList.size(); i++){
            UserFile userFile = userFileList.get(i);
            QiwenFile qiwenFile = new QiwenFile(userFile.getFilePath(), userFile.getFileName(), false);
            String filePath = qiwenFile.getPath();

            Queue<String> queue = new LinkedList<>();

            String[] strArr = filePath.split(QiwenFile.separator);
            for (int j = 0; j < strArr.length; j++){
                if (!"".equals(strArr[j]) && strArr[j] != null){
                    queue.add(strArr[j]);
                }

            }
            if (queue.size() == 0){
                continue;
            }

            resultTreeNode = fileDealComp.insertTreeNode(resultTreeNode, id++, QiwenFile.separator, queue);


        }
        List<TreeNode> treeNodeList = resultTreeNode.getChildren();
        Collections.sort(treeNodeList, (o1, o2) -> {
            long i = o1.getId() - o2.getId();
            return (int) i;
        });
//        result.setSuccess(true);
//        result.setData(resultTreeNode);
        return CommonResult.successResponse(resultTreeNode);
    }

    /**
     * 文件移动: 可以移动文件或者目录
     */
    @RequestMapping(value = "/movefile", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult<String> moveFile(@RequestBody MoveFileDTO moveFileDto) {

        UserFile userFile = userFileService.getById(moveFileDto.getUserFileId());
        String oldfilePath = userFile.getFilePath();
        String newfilePath = moveFileDto.getFilePath();
        String fileName = userFile.getFileName();
        String extendName = userFile.getExtendName();
        if (StringUtil.isEmpty(extendName)) {
            QiwenFile qiwenFile = new QiwenFile(oldfilePath, fileName, true);
            if (newfilePath.startsWith(qiwenFile.getPath() + QiwenFile.separator) || newfilePath.equals(qiwenFile.getPath())) {
                return CommonResult.errorResponse("原路径与目标路径冲突，不能移动");
            }
        }

        userFileService.updateFilepathByUserFileId(moveFileDto.getUserFileId(), newfilePath, String.valueOf(StpUtil.getLoginId()));

        fileDealComp.deleteRepeatSubDirFile(newfilePath, String.valueOf(StpUtil.getLoginId()));
        return CommonResult.successResponse("操作成功！");

    }

    /**
     *批量移动文件: 可以同时选择移动多个文件或者目录
     */
    @RequestMapping(value = "/batchmovefile", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult<String> batchMoveFile(@RequestBody BatchMoveFileDTO batchMoveFileDto) {

        String newfilePath = batchMoveFileDto.getFilePath();

        String userFileIds = batchMoveFileDto.getUserFileIds();
        String[] userFileIdArr = userFileIds.split(",");

        for (String userFileId : userFileIdArr) {
            UserFile userFile = userFileService.getById(userFileId);
            if (StringUtil.isEmpty(userFile.getExtendName())) {
                QiwenFile qiwenFile = new QiwenFile(userFile.getFilePath(), userFile.getFileName(), true);
                if (newfilePath.startsWith(qiwenFile.getPath() + QiwenFile.separator) || newfilePath.equals(qiwenFile.getPath())) {
                    return CommonResult.errorResponse("原路径与目标路径冲突，不能移动");
                }
            }
            userFileService.updateFilepathByUserFileId(userFile.getUserFileId(), newfilePath, String.valueOf(StpUtil.getLoginId()));
        }
        return CommonResult.successResponse("批量移动文件成功");
    }

    /**
     * 文件复制: 可以复制文件或者目录
     */
    @RequestMapping(value = "/copyfile", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult<String> copyFile(@RequestBody CopyFileDTO copyFileDTO) {
        String userId = String.valueOf(StpUtil.getLoginId());
        String filePath = copyFileDTO.getFilePath();
        String userFileIds = copyFileDTO.getUserFileIds();
        String[] userFileIdArr = userFileIds.split(",");
        for (String userFileId : userFileIdArr) {
            UserFile userFile = userFileService.getById(userFileId);
            String oldfilePath = userFile.getFilePath();
            String fileName = userFile.getFileName();
            if (userFile.isDirectory()) {
                QiwenFile qiwenFile = new QiwenFile(oldfilePath, fileName, true);
                if (filePath.startsWith(qiwenFile.getPath() + QiwenFile.separator) || filePath.equals(qiwenFile.getPath())) {
                    return CommonResult.errorResponse("原路径与目标路径冲突，不能复制");
                }
            }
            userFileService.userFileCopy(String.valueOf(StpUtil.getLoginId()), userFileId, filePath);
            fileDealComp.deleteRepeatSubDirFile(filePath, userId);
        }

        return CommonResult.successResponse("操作成功");

    }

    // 创建文件接口，用于上传文件
    @ResponseBody
    @RequestMapping(value = "/createFile", method = RequestMethod.POST)
    public CommonResult<Object> createFile(@Valid @RequestBody CreateFileDTO createFileDTO) {
        try {
            // 获取当前用户ID
            String userId = String.valueOf(StpUtil.getLoginId());
            // 提取文件路径、名称和扩展名
            String filePath = createFileDTO.getFilePath();
            String fileName = createFileDTO.getFileName();
            String extendName = createFileDTO.getExtendName();

            // 检查是否存在同名文件
            List<UserFile> userFiles = userFileService.selectSameUserFile(fileName, filePath, extendName, userId);
            if (userFiles != null && !userFiles.isEmpty()) {
                return CommonResult.errorResponse("同名文件已存在");
            }

            // 生成唯一标识符
            String uuid = UUID.randomUUID().toString().replaceAll("-", "");

            // 根据文件扩展名选择模板文件路径
            String templateFilePath = "";
            if ("docx".equals(extendName)) {
                templateFilePath = "template/Word.docx";
            } else if ("xlsx".equals(extendName)) {
                templateFilePath = "template/Excel.xlsx";
            } else if ("pptx".equals(extendName)) {
                templateFilePath = "template/PowerPoint.pptx";
            } else if ("txt".equals(extendName)) {
                templateFilePath = "template/Text.txt";
            } else if ("drawio".equals(extendName)) {
                templateFilePath = "template/Drawio.drawio";
            }

            // 获取模板文件的绝对路径
            String url2 = ClassUtils.getDefaultClassLoader().getResource("static/" + templateFilePath).getPath();
            url2 = URLDecoder.decode(url2, "UTF-8");

            // 读取模板文件
            FileInputStream fileInputStream = new FileInputStream(url2);
            Copier copier = ufopFactory.getCopier();
            CopyFile copyFile = new CopyFile();
            copyFile.setExtendName(extendName);
            String fileUrl = copier.copy(fileInputStream, copyFile);

            // 创建文件信息对象
            FileBean fileBean = new FileBean();
            fileBean.setFileId(IdUtil.getSnowflakeNextIdStr());
            fileBean.setFileSize(0L);
            fileBean.setFileUrl(fileUrl);
            fileBean.setStorageType(storageType);
            fileBean.setIdentifier(uuid);
            fileBean.setCreateTime(DateUtil.getCurrentTime());
            fileBean.setCreateUserId(String.valueOf(StpUtil.getLoginId()));
            fileBean.setFileStatus(1);

            // 保存文件信息
            boolean saveFlag = fileService.save(fileBean);

            // 如果文件信息保存成功，则创建用户文件关联信息
            UserFile userFile = new UserFile();
            if (saveFlag) {
                userFile.setUserFileId(IdUtil.getSnowflakeNextIdStr());
                userFile.setUserId(userId);
                userFile.setFileName(fileName);
                userFile.setFilePath(filePath);
                userFile.setDeleteFlag(0);
                userFile.setIsDir(0);
                userFile.setExtendName(extendName);
                userFile.setUploadTime(DateUtil.getCurrentTime());
                userFile.setFileId(fileBean.getFileId());
                userFile.setCreateTime(DateUtil.getCurrentTime());
                userFile.setCreateUserId(String.valueOf(StpUtil.getLoginId()));
                userFileService.save(userFile);
            }

            // 返回成功信息
            return CommonResult.successResponse("文件创建成功");
        } catch (Exception e) {
            // 记录错误日志并返回失败信息
            log.error(e.getMessage());
            return CommonResult.errorResponse(e.getMessage());
        }
    }


    /**
     * 普通文件上传
     */
    @PostMapping("/upload")
    @Transactional
    public CommonResult<List<String>> uploadFile(HttpServletRequest request){

        if (!(request instanceof MultipartHttpServletRequest)) {
            return CommonResult.errorResponse("请求不是 multipart 类型");
        }

        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;

        // 获取单个文件
        MultipartFile file = multipartRequest.getFile("file");

        if (file == null || file.isEmpty()) {
            return CommonResult.errorResponse("上传文件为空");
        }

        List<UploadFileResult> resp = fileService.uploadFile(request);

        List<String> fileUrls = resp.stream()
                .map(UploadFileResult::getFileUrl) // 提取 fileUrl
                .collect(Collectors.toList());    // 转为 List

        return CommonResult.successResponse(fileUrls,"上传并写入成功");
    }

    @Operation(summary = "批量删除文件", description = "批量删除文件", tags = {"file"})
    @PostMapping("/batchdeletefile")
    public CommonResult<String> deleteImageByIds(@RequestBody BatchDeleteFileDTO batchDeleteFileDto) {
        String userFileIds = batchDeleteFileDto.getUserFileIds();
        String[] userFileIdList = userFileIds.split(",");
        userFileService.update(new UpdateWrapper<UserFile>().lambda().set(UserFile::getDeleteFlag, 1).in(UserFile::getUserFileId, Arrays.asList(userFileIdList)));
        for (String userFileId : userFileIdList) {
            executor.execute(()->{
                userFileService.deleteUserFile(userFileId, SessionUtil.getUserId());
            });
        }

        return CommonResult.successResponse("批量删除文件成功");
    }

}
