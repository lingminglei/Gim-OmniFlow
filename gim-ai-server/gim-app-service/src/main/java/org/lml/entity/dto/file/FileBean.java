package org.lml.entity.dto.file;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.qiwenshare.common.util.DateUtil;
import com.qiwenshare.ufop.operation.upload.domain.UploadFileResult;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 *  文件对象 Dto
 * </p>
 *
 * @author lml
 * @since 2025-07-21
 */
@Data
@TableName("file")
public class FileBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "file_id", type = IdType.ASSIGN_UUID)
    private String fileId;

    /**
     * 创建时间
     */
    private String createTime;

    /**
     * 创建用户id
     */
    private String createUserId;

    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * 文件状态(0-失效，1-生效)
     */
    private Integer fileStatus;

    /**
     * 文件url
     */
    private String fileUrl;

    /**
     * md5唯一标识
     */
    private String identifier;

    /**
     * 修改时间
     */
    private String modifyTime;

    /**
     * 修改用户id
     */
    private String modifyUserId;

    /**
     * 存储类型
     */
    private Integer storageType;

    public FileBean(){

    }

    public FileBean(UploadFileResult uploadFileResult) {
        this.fileId = IdUtil.getSnowflakeNextIdStr();
        this.fileUrl = uploadFileResult.getFileUrl();
        this.fileSize = uploadFileResult.getFileSize();
        this.fileStatus = 1;
        this.storageType = uploadFileResult.getStorageType().getCode();
        this.identifier = uploadFileResult.getIdentifier();
        this.createTime = DateUtil.getCurrentTime();

    }

    public FileBean(String fileUrl, Long fileSize, Integer storageType, String identifier, String userId) {
        this.fileId = IdUtil.getSnowflakeNextIdStr();
        this.fileUrl = fileUrl;
        this.fileSize = fileSize;
        this.fileStatus = 1;
        this.storageType = storageType;
        this.identifier = identifier;
        this.createTime = DateUtil.getCurrentTime();
        this.createUserId = userId;

    }

}
