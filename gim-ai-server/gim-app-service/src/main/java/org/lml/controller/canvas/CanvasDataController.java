package org.lml.controller.canvas;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import org.lml.common.result.CommonResult;
import org.lml.entity.dto.canvas.CanvasData;
import org.lml.entity.req.canvas.CanvasDataReq;
import org.lml.service.canvas.ICanvasDataService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.stereotype.Controller;

import javax.annotation.Resource;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author lml
 * @since 2025-12-15
 */
@Controller
@RequestMapping("/canvasData")
public class CanvasDataController {

    @Resource
    private ICanvasDataService iCanvasDataService;

    /**
     * 保存画布版本
     */
    @PostMapping("/save")
    public CommonResult<String> save(@RequestBody @Validated CanvasDataReq canvasDataReq) {

        String userId = String.valueOf(StpUtil.getLoginId());

        canvasDataReq.setUserId(Long.valueOf(userId));

        CanvasData canvasData = new CanvasData();
        BeanUtil.copyProperties(canvasDataReq, canvasData);

        boolean res = iCanvasDataService.save(canvasData);

        if(res){
            return CommonResult.errorResponse("操作成功!");
        }else{
            return CommonResult.errorResponse("保存失败");
        }
    }

}
