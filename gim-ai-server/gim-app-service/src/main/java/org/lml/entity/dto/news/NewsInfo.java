package org.lml.entity.dto.news;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * <p>
 * 多平台新闻聚合表
 * </p>
 *
 * @author lml
 * @since 2025-11-14
 */
@TableName("news_info")
@Data
public class NewsInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 来源标识，如 douyin, zhihu, weibo, netease
     */
    private String sourceCode;

    /**
     * 来源名称展示，如 抖音、知乎、微博
     */
    private String sourceName;

    /**
     * 来源图标地址
     */
    private String sourceIcon;

    /**
     * 来源侧唯一ID，如 docid、qid、newsId，用于去重
     */
    private String sourceUniqueId;

    /**
     * 原始新闻链接
     */
    private String url;

    /**
     * 标题
     */
    private String title;

    /**
     * 摘要或简介内容
     */
    private String summary;

    /**
     * 完整正文内容（可选）
     */
    private String content;

    /**
     * 封面图URL
     */
    private String coverImage;

    /**
     * 分类标签，如 热榜、科技、娱乐 等
     */
    private String tag;

    /**
     * 作者或发布者
     */
    private String author;

    /**
     * 发布时间
     */
    private Date publishTime;

    /**
     * 热度值，如浏览量、点赞数等
     */
    private Long hotValue;

    /**
     * 评论数
     */
    private Integer commentCount;

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 抓取时间
     */
    private Date fetchTime;

    /**
     * 状态 1=有效 0=失效
     */
    private Integer status;

    /**
     * 原始完整数据JSON，方便兼容多源
     */
    private String extraJson;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 更新人
     */
    private String updateBy;

}
