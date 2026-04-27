package org.lml.thirdService.knowledgeService.controller;

import lombok.extern.slf4j.Slf4j;
import org.lml.thirdService.knowledgeService.req.ChatMessageReq;
import org.lml.thirdService.knowledgeService.service.ChatService;
import org.lml.thirdService.knowledgeService.service.DocumentIngestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/chat")
@Slf4j
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Resource
    private DocumentIngestService documentIngestService;

    @PostMapping("/ask")
    public String ask(@RequestBody ChatMessageReq chatMessageReq) {
        return chatService.askWithKnowledgeBase(chatMessageReq.getQuestion(),
                chatMessageReq.getKnowledgeCode(),
                chatMessageReq.getUserId());
    }


}
