package org.lml.entity.resp;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import javax.persistence.*;

/**
 * 上传文件DTO
 */
@Data
public class UploadFileDTO {

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 文件名
     */
    private String filename;

    /**
     * 切片数量
     */
    private int chunkNumber;

    /**
     * 切片大小
     */
    private long chunkSize;
    /**
     * 相对路径
     */
    private String relativePath;

    /**
     * 所有切片
     */
    private int totalChunks;
    /**
     * 总大小
     */
    private long totalSize;
    /**
     * 当前切片大小
     */
    private long currentChunkSize;
    /**
     * md5码
     */
    private String identifier;

}
