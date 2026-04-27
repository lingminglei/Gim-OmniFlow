package org.lml.common.enums.pay;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务计费类型枚举
 * 用于统一管理系统内所有涉及积分消耗的业务逻辑
 */
@Getter
@AllArgsConstructor
public enum BizPricingType {

    /**
     * 智能对话业务
     * 计算规则：1积分 = 1000 token
     */
    CHAT_TOKEN("CHAT_TOKEN", 1, "对话 (消耗积分/1000token)"),

    /**
     * AI 图片生成业务
     * 计算规则：固定扣除 10 积分/张
     */
    IMAGE_GEN("IMAGE_GEN", 10, "生成图片"),

    /**
     * AI 视频生成业务
     * 计算规则：100 积分 / 10s (不足 10s 按 10s 计算或根据实际时长阶梯收费)
     */
    VIDEO_GEN("VIDEO_GEN", 100, "生成视频 (10s)"),

    /**
     * 网盘存储计费
     * 计算规则：每日结算，消耗积分 = (占用容量GB * 单价) / 365
     */
    STORAGE_FEE("STORAGE_FEE", 1, "存储费 (积分/GB/天)");

    /**
     * 业务类型唯一标识
     * 对应数据库 transaction_logs 表中的 biz_type 字段
     */
    private final String bizType;

    /**
     * 基础消耗积分数值
     * 作为计费策略中的核心参数
     */
    private final int baseCredit;

    /**
     * 业务中文描述
     * 用于前端展示或系统审计日志显示
     */
    private final String description;
}