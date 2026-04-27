package org.lml.thirdService.knowledgeService.req;

import lombok.Data;

import java.util.List;

@Data
public class ChatMessageReq {

    private String question;

    private List<String> knowledgeCode;

    private String userId;

}
