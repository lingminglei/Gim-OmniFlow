package org.lml.controller.file;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import org.lml.common.result.CommonResult;
import org.lml.entity.dto.file.DeleteRecoveryFileDTO;
import org.lml.entity.dto.file.RecoveryFile;
import org.lml.entity.dto.file.RecoveryFileListVo;
import org.lml.entity.dto.file.RestoreFileDTO;
import org.lml.service.file.IRecoveryFileService;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Controller;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author lml
 * @since 2025-07-23
 */
@Controller
@RequestMapping("/recoveryFile")
public class RecoveryFileController {

    @Resource
    private IRecoveryFileService recoveryFileService;

    @Operation(summary = "回收文件列表", description = "回收文件列表", tags = {"recoveryfile"})
    @GetMapping(value = "/list")
    @ResponseBody
    public CommonResult<List<RecoveryFileListVo>> getRecoveryFileList() {

        String userId  = String.valueOf(StpUtil.getLoginId());
        List<RecoveryFileListVo> recoveryFileList = recoveryFileService.selectRecoveryFileList(userId);
        return CommonResult.successResponse(recoveryFileList);
    }

    @Operation(summary = "删除回收文件", description = "删除回收文件", tags = {"recoveryfile"})
    @PostMapping( "/deleterecoveryfile")
    @ResponseBody
    public CommonResult<String> deleteRecoveryFile(@RequestBody DeleteRecoveryFileDTO deleteRecoveryFileDTO) {
        RecoveryFile recoveryFile = recoveryFileService.getOne(new QueryWrapper<RecoveryFile>().lambda().eq(RecoveryFile::getUserFileId, deleteRecoveryFileDTO.getUserFileId()));

        recoveryFileService.removeById(recoveryFile.getRecoveryFileId());
        return CommonResult.successResponse("删除成功");
    }

    @Operation(summary = "还原文件", description = "还原文件", tags = {"recoveryfile"})
    @PostMapping(value = "/restorefile")
    @ResponseBody
    public CommonResult<String> restoreFile(@RequestBody RestoreFileDTO restoreFileDto) {
        String userId  = String.valueOf(StpUtil.getLoginId());
        recoveryFileService.restorefile(restoreFileDto.getDeleteBatchNum(), restoreFileDto.getFilePath(), userId);
        return CommonResult.successResponse("还原成功！");
    }

}
