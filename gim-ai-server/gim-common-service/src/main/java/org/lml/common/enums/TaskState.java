package org.lml.common.enums;

import lombok.Getter;

/**
 * 任务执行状态枚举
 */
@Getter
public enum TaskState {

    /**
     * 初始化：不校验
     *
     * 已提交：前置：初始化
     *
     * 处理中：前置：初始化、已提交
     *
     * 成功：前置：处理中
     *
     * 失败：前置：初始化、已提交、处理中
     */

    INIT("INIT","初始化"),
    SUBMITTED("SUBMITTED","已提交"),
    PROCESSING("PROCESSING","处理中"),
    SUCCESS("SUCCESS","成功"),
    FAIL("FAIL","失败");

    private final String code;
    private final String description;

    TaskState(String code,String description) {
        this.code = code;
        this.description = description;
    }

    // 可以添加逻辑判断是否允许状态转换
    public boolean canTransitionTo(TaskState nextStatus) {
        switch (this) {
            case INIT: return nextStatus == SUBMITTED;
            case SUBMITTED: return nextStatus == PROCESSING;
            case PROCESSING: return nextStatus == SUCCESS || nextStatus == FAIL;
            default: return false; // 终态不可更改
        }
    }

}
