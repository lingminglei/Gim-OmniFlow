package org.lml.controller.agent;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qiwenshare.ufop.operation.upload.domain.UploadFileResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.lml.common.result.CommonResult;
import org.lml.entity.dto.agent.UserKnowledgeMapping;
import org.lml.entity.dto.agent.VectorFileInfo;
import org.lml.entity.req.agent.KnowledgeCreateReq;
import org.lml.entity.req.agent.KnowledgeRenameReq;
import org.lml.entity.resp.agent.UserKnowledgeMappingResp;
import org.lml.entity.resp.agent.VectorFileInfoResp;
import org.lml.service.agent.IUserVectorAndFileAssociationService;
import org.lml.service.agent.IVectorFileInfoService;
import org.lml.service.file.IFileService;
import org.lml.thirdService.knowledgeService.service.DocumentIngestService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import java.util.*;

import static org.lml.utils.CommonUtils.*;
import static org.lml.utils.KnowledgeBaseStyleGenerator.generateRandomStyle;

/**
 * 知识库 Controller 层
 */
@Slf4j
@RestController
@RequestMapping("/knowledge")
public class KnowledgeController {

    @Resource
    private DocumentIngestService documentIngestService;

    @Resource
    private IUserVectorAndFileAssociationService iUserVectorAndFileAssociationService;

    @Resource
    private IFileService iFileService;

    @Resource
    private IVectorFileInfoService vectorFileInfoService;

    /**
     * 创建知识库
     */
    @PostMapping("/create")
    @Transactional
    public CommonResult<Map<String,String>> createKnowledge(@RequestBody KnowledgeCreateReq knowledgeCreateReq){

        String knowledgeCode = "Gim_"+generateShortUniqueId();

        log.info("生成知识库 Code={}", knowledgeCode);

        UserKnowledgeMapping result = iUserVectorAndFileAssociationService.isKonwLedgeNameExist(knowledgeCreateReq.getName());

        if (ObjectUtil.isNotNull(result)) {
            return CommonResult.errorResponse("知识库名称已存在");
        }

        boolean initVectorCollection = documentIngestService.initVectorCollection(knowledgeCode);

        if(initVectorCollection){

            UserKnowledgeMapping verticalFileInfo = new UserKnowledgeMapping();
            verticalFileInfo.setKnowledgeCode(knowledgeCode);
            verticalFileInfo.setUserId(String.valueOf(StpUtil.getLoginId()));
            verticalFileInfo.setCreateTime(new Date());
            verticalFileInfo.setUpdateTime(new Date());
            verticalFileInfo.setName(knowledgeCreateReq.getName());
            verticalFileInfo.setDescription(knowledgeCreateReq.getDescription());

            Map<String,String> maps =  generateRandomStyle();

            verticalFileInfo.setBgColor(maps.get("bgColor"));
            verticalFileInfo.setIcon(maps.get("icon"));

            iUserVectorAndFileAssociationService.save(verticalFileInfo);

            Map<String,String> map = new HashMap<>();
            map.put("knowledgeCode",knowledgeCode);
            return CommonResult.successResponse(map,"操作成功");
        }else{
            return CommonResult.errorResponse("操作失败");
        }

    }

    /**
     * 上传文件到知识库
     */
    @PostMapping("/upload")
    @Transactional
    public CommonResult<String> uploadFile(@RequestParam("knowledgeCode") String knowledgeCode,
                                           HttpServletRequest request){

        if (!(request instanceof MultipartHttpServletRequest)) {
            return CommonResult.errorResponse("请求不是 multipart 类型");
        }

        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;

        // 获取单个文件
        MultipartFile file = multipartRequest.getFile("file");

        if (file == null || file.isEmpty()) {
            return CommonResult.errorResponse("上传文件为空");
        }

        log.info("上传文件到知识库, knowledgeCode={}, userId={}", knowledgeCode);
        List<UploadFileResult> resp = iFileService.uploadFile(request);

        boolean result = documentIngestService.ingestFromFile(file, knowledgeCode);

        if(result){

            for(UploadFileResult uploadFileResult : resp){

                VectorFileInfo vectorFileInfo = new VectorFileInfo();
                vectorFileInfo.setFileName(uploadFileResult.getFileName());
                vectorFileInfo.setFilePath(uploadFileResult.getFileUrl());
                vectorFileInfo.setFileSize(uploadFileResult.getFileSize());
                vectorFileInfo.setFileType(uploadFileResult.getExtendName());
                vectorFileInfo.setUserId(String.valueOf(StpUtil.getLoginId()));
                vectorFileInfo.setKnowledgeCode(knowledgeCode);
                vectorFileInfo.setCreateTime(new Date());
                vectorFileInfo.setUpdateTime(new Date());

                vectorFileInfoService.save(vectorFileInfo);
            }
        }

        return CommonResult.successResponse("上传并写入成功");
    }

    /**
     * 查询知识库列表
     */
    @GetMapping("/queryKnowledgeList")
    public CommonResult<Page<UserKnowledgeMappingResp>> queryKnowledgeList(@RequestParam(required = false, defaultValue = "1")Integer current,
                                                                       @RequestParam(required = false, defaultValue = "10")Integer size){

        log.info("查询知识库列表");

        // 创建分页对象
        Page<UserKnowledgeMapping> page = new Page<>(current, size);
        QueryWrapper<UserKnowledgeMapping> qws = new QueryWrapper<>();
        qws.eq("status",1);
        qws.eq("user_id",String.valueOf(StpUtil.getLoginId()));
        // 执行分页查询
        IPage<UserKnowledgeMapping> list =  iUserVectorAndFileAssociationService.page(page, qws);

        List<UserKnowledgeMapping> list1 = list.getRecords();

        List<UserKnowledgeMappingResp> list2 = new ArrayList<UserKnowledgeMappingResp>();

        for(UserKnowledgeMapping userKnowledgeMapping : list1){
            QueryWrapper<VectorFileInfo> qw = new QueryWrapper<>();
            qw.eq("knowledge_code",userKnowledgeMapping.getKnowledgeCode());
            qw.eq("user_id",userKnowledgeMapping.getUserId());

            List<VectorFileInfo> vectorFileInfoList = vectorFileInfoService.list(qw);

            UserKnowledgeMappingResp resp = new UserKnowledgeMappingResp();
            BeanUtil.copyProperties(userKnowledgeMapping,resp);
            resp.setFileCount(vectorFileInfoList.size());
            resp.setUpdateTimeStr(formatTimeAgo(userKnowledgeMapping.getUpdateTime()));

            list2.add(resp);
        }

        Page<UserKnowledgeMappingResp> resp = new Page<>();
        resp.setRecords(list2);
        resp.setTotal(list.getTotal());

        return CommonResult.successResponse(resp);
    }

    /**
     * 删除知识库
     */
    @GetMapping("/delete")
    public CommonResult<String> deleteKnowledge(@RequestParam("knowledgeCode") String knowledgeCode){

        log.info("删除知识库, knowledgeCode={}", knowledgeCode);

        QueryWrapper<UserKnowledgeMapping> qw = new QueryWrapper<>();
        qw.eq("knowledge_code", knowledgeCode);
        qw.eq("status",1);

        List<UserKnowledgeMapping> list = iUserVectorAndFileAssociationService.list(qw);

        if(list.isEmpty() || list.size() <=0){
            return CommonResult.errorResponse("操作失败，知识库不存在");
        }

        UserKnowledgeMapping userKnowledgeMapping = list.get(0);

        userKnowledgeMapping.setStatus(0);

        boolean result = iUserVectorAndFileAssociationService.updateById(userKnowledgeMapping);

        if(result){
            return CommonResult.successResponse("操作成功！");
        }else{
            return CommonResult.errorResponse("操作失败！");
        }
    }

    /**
     * 查询知识库文件
     */
    @GetMapping("/queryKnowledgeFileList")
    public CommonResult<List<VectorFileInfoResp>> queryKnowledgeFileList(@RequestParam(value = "knowledgeCode",required = false) String knowledgeCode){

        if(ObjectUtil.isNull(knowledgeCode) || StringUtils.isBlank(knowledgeCode)){
            return CommonResult.errorResponse("查询参数有误！");
        }
        log.info("查询知识库文件, knowledgeCode={}", knowledgeCode);

        QueryWrapper<VectorFileInfo> qw = new QueryWrapper<>();
        qw.eq("knowledge_code", knowledgeCode);
        qw.eq("status",1);

        List<VectorFileInfo> list = vectorFileInfoService.list(qw);


        List<VectorFileInfoResp> list1 = new ArrayList<>();

        for(VectorFileInfo vectorFileInfo : list){
            VectorFileInfoResp vectorFileInfoResp = new VectorFileInfoResp();
            BeanUtil.copyProperties(vectorFileInfo,vectorFileInfoResp);
            vectorFileInfoResp.setFileSizeStr(formatSize(vectorFileInfo.getFileSize()));
            vectorFileInfoResp.setStatusType(vectorFileInfo.getStatus() == 1 ? "已处理" :"处理中");
            list1.add(vectorFileInfoResp);
        }

        return CommonResult.successResponse(list1);
    }

    /**
     * 知识库信息编辑
     */
    @PostMapping("/edit")
    public CommonResult<String> renameKnowledge(@RequestBody KnowledgeRenameReq req){
        /**
         * TODO: 这里要加知识库名称修改，要判断自己的知识库 除该知识库外，是否有相同的知识库名称；
         */
        QueryWrapper<UserKnowledgeMapping> qw = new QueryWrapper<>();
        qw.eq("name",req.getName());
        qw.eq("status","1");
        qw.eq("user_id",String.valueOf(StpUtil.getLoginId()));
        List<UserKnowledgeMapping> list = iUserVectorAndFileAssociationService.list(qw);

        Boolean isExist = false;

        for(UserKnowledgeMapping userKnowledgeMapping : list){
            if(userKnowledgeMapping.getKnowledgeCode().equals(req.getKnowledgeCode())){
                continue;
            }

            if(userKnowledgeMapping.getName().equals(req.getName())){
                isExist = true;
            }
        }

        if(isExist){
            return CommonResult.errorResponse("操作失败，知识库名称已存在");
        }

        QueryWrapper<UserKnowledgeMapping> qws = new QueryWrapper<>();
        qws.eq("knowledge_code",req.getKnowledgeCode());
        List<UserKnowledgeMapping> dataList = iUserVectorAndFileAssociationService.list(qws);

        if(ObjectUtil.isEmpty(dataList) || dataList.size() <=0){
            return CommonResult.errorResponse("操作失败，知识库不存在");
        }

        UserKnowledgeMapping data = dataList.get(0);

        data.setName(req.getName());

        data.setDescription(req.getDescription());

        boolean result1 = iUserVectorAndFileAssociationService.updateById(data);

        if(result1){
            return CommonResult.successResponse("操作成功！");
        }else {
            return CommonResult.errorResponse("操作失败！");
        }
    }

    /**
     * 知识库文件删除
     */
    @GetMapping("/deleteFile")
    public CommonResult<String> deleteFile(@RequestParam("fileId") String fileId){
       VectorFileInfo vectorFileInfo = vectorFileInfoService.getById(fileId);

       if(ObjectUtil.isNull(vectorFileInfo)){
           return CommonResult.errorResponse("操作失败，文件不存在");
       }

       vectorFileInfo.setStatus(0);

       boolean result = vectorFileInfoService.updateById(vectorFileInfo);

       if( result){
           return CommonResult.successResponse("操作成功！");
       }else{
           return CommonResult.errorResponse("操作失败！");
       }

    }
}
