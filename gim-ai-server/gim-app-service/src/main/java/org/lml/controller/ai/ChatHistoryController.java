package org.lml.controller.ai;

import org.lml.common.result.CommonResult;
import org.lml.thirdService.ws.entity.resp.ChatHistorySessionResp;
import org.lml.thirdService.ws.service.WsChatHistoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * WebSocket 聊天历史控制器。
 * 提供给前端查询当前登录用户全部会话历史的接口。
 */
@RestController
@RequestMapping("/ai/chat/history")
public class ChatHistoryController {

    @Resource
    private WsChatHistoryService wsChatHistoryService;

    /**
     * 查询当前登录用户的全部会话历史。
     */
    @GetMapping("/sessions")
    public CommonResult<List<ChatHistorySessionResp>> querySessions() {
        return CommonResult.successResponse(wsChatHistoryService.queryCurrentUserSessions());
    }
}
