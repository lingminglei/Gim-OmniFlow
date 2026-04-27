package org.lml.entity.dto.file;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.qiwenshare.common.util.DateUtil;
import lombok.Data;
import org.lml.utils.file.QiwenFile;

import java.io.Serializable;

/**
 * <p>
 *  用户文件 Dto
 * </p>
 *
 * @author lml
 * @since 2025-07-21
 */
@Data
@TableName("user_file")
public class UserFile implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "user_file_id", type = IdType.ASSIGN_UUID)
    private String userFileId;

    /**
     * 创建时间
     */
    private String createTime;

    /**
     * 创建用户id
     */
    private String createUserId;

    /**
     * 删除批次号
     */
    private String deleteBatchNum;

    /**
     * 删除标识(0-未删除，1-已删除)
     */
    private Integer deleteFlag;

    /**
     * 删除时间
     */
    private String deleteTime;

    /**
     * 扩展名
     */
    private String extendName;

    /**
     * 文件id
     */
    private String fileId;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 是否是目录(0-否,1-是)
     */
    private Integer isDir;

    /**
     * 修改时间
     */
    private String modifyTime;

    /**
     * 修改用户id
     */
    private String modifyUserId;

    /**
     * 修改时间
     */
    private String uploadTime;

    /**
     * 用户id
     */
    private String userId;
    public UserFile() {};
    public UserFile(QiwenFile qiwenFile, String userId, String fileId) {
        this.userFileId = IdUtil.getSnowflakeNextIdStr();
        this.userId = userId;
        this.fileId = fileId;
        this.filePath = qiwenFile.getParent();
        this.fileName = qiwenFile.getNameNotExtend();
        this.extendName = qiwenFile.getExtendName();
        this.isDir = qiwenFile.isDirectory() ? 1 : 0;
        String currentTime = DateUtil.getCurrentTime();
        this.setUploadTime(currentTime);
        this.setCreateUserId(userId);
        this.setCreateTime(currentTime);
        this.deleteFlag = 0;
    }

    public boolean isDirectory() {
        return this.isDir == 1;
    }

    public boolean isFile() {
        return this.isDir == 0;
    }


}
