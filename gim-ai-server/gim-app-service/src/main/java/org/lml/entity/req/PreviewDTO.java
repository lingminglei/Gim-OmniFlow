package org.lml.entity.req;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
@Schema(name = "预览文件DTO",required = true)
public class PreviewDTO {
    private String userFileId;
    @Schema(description="批次号")
    private String shareBatchNum;
    @Schema(description="提取码")
    private String extractionCode;
    private String isMin;
    private Integer platform;
    private String url;
    private String token;

    //================= 知识库处理
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 文件名
     */
    private String fileName;

    /**
     * 物理路径/OSS地址
     */
    private String filePath;

    /**
     * 文件类型（如 PDF、DOCX）
     */
    private String fileType;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 上传用户ID
     */
    private String userId;

    /**
     * 上传用户昵称
     */
    private String userName;

    /**
     * 知识库编码
     */
    private String knowledgeCode;

}
