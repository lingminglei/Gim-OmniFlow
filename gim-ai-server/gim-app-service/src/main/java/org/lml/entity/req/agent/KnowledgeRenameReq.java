package org.lml.entity.req.agent;

import lombok.Data;

@Data
public class KnowledgeRenameReq {

    private String knowledgeCode;

    private String description;

    private String name;
}
