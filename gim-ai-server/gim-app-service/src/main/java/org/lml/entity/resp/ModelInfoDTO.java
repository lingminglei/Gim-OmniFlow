package org.lml.entity.resp;

import lombok.Builder;
import lombok.Data;

/**
 * AI 模型信息传输对象 (DTO)。
 * 用于对外接口返回模型的基本信息，隐藏敏感的 ApiKey 等字段。
 */
@Data
@Builder
public class ModelInfoDTO {
    /** 模型别名 (Key) */
    private String alias;
    /** 模型名称 (如 gpt-3.5-turbo) */
    private String modelName;
    /** 模型描述 (如 通用对话模型) */
    private String description;
    /** 模型 Base URL */
    private String baseUrl;
}
