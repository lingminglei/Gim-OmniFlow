package org.lml.thirdService.mq.streamRocketMq.entity.request;

import lombok.Data;

@Data
public class CreateImageRequest {

    private String prompt;

    private String style;

    private String filePath;
}
