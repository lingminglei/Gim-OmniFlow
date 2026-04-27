package org.lml.entity.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 *
 * @Description: 批量下载文件DTO
 */
@Data
@Schema(name = "批量下载文件DTO",required = true)
public class BatchDownloadFileDTO {
    @Schema(description="文件集合", required = true)
    private String userFileIds;
    @Schema(description="批次号")
    private String shareBatchNum;
    @Schema(description="提取码")
    private String extractionCode;

}
