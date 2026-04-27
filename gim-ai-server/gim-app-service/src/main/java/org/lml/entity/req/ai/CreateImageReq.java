package org.lml.entity.req.ai;

import lombok.Data;

@Data
public class CreateImageReq {

    private String prompt;

    private String style;

    private String filePath;
}
