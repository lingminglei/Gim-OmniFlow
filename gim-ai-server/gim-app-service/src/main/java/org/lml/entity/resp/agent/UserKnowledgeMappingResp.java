package org.lml.entity.resp.agent;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Data
public class UserKnowledgeMappingResp {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

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
     * 状态（1-正常，2-删除，3-待处理）
     */
    private Integer status;

    /**
     * 上传时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    /**
     * 知识库名称
     */
    private String name;

    /**
     * 知识库描述
     */
    private String description;

    /**
     * 知识库文件数量
     */
    private Integer fileCount;

    /**
     * 查询当前时间描述
     */
    private String updateTimeStr;

    /**
     * 知识库图标
     */
    private String icon;

    /**
     * 知识库颜色
     */
    private String bgColor;
}
