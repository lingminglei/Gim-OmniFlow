package org.lml.entity.dto.agent;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * <p>
 * Agent-向量文件信息表
 * </p>
 *
 * @author lml
 * @since 2025-07-28
 */
@TableName("vector_file_info")
@Data
public class VectorFileInfo implements Serializable {

    private static final long serialVersionUID = 1L;

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

    /**
     * 向量集合名（Qdrant collection）
     */
    private String vectorCollection;

    /**
     * 切分后生成的向量段数量
     */
    private Integer chunkCount;

    /**
     * 状态（1-正常，2-删除，3-待处理）
     */
    private Integer status;

    /**
     * 上传时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;


}
