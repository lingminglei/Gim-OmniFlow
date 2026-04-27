package org.lml.thirdService.comfyUi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ComfyTaskResponse {
    
    // 任务唯一标识
    private String promptId;
    
    // 状态枚举：PENDING (排队), RUNNING (执行中), COMPLETED (完成), FAILED (失败)
    private String status;
    
    // 生成结果的 URL (仅在 COMPLETED 时有效)
    private String imageUrl;
    
    // 进度 (0-1.0)
    private double progress;
    
    // 错误信息 (仅在 FAILED 时有效)
    private String errorMessage;
}
