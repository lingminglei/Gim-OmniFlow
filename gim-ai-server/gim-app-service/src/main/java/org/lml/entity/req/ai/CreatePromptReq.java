package org.lml.entity.req.ai;

import lombok.Data;

@Data
public class CreatePromptReq {

    /**
     * 用户标题
     */
    private String title;

    /**
     * 用户提示词
     */
    private String userPrompt;

    /**
     * 视频风格
     */
    private String videoStyle;

    /**
     * 视频长度: 分片个数
     */
    private Integer videoLength;

}
