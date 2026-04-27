package org.lml.AiGateWay.enums;

import lombok.Getter;

/**
 * 支持的 AI 厂商枚举。
 */
@Getter
public enum VideoProvider {

    /** Sora 视频生成。 */
    SORA_TO_VIDEO("SORA_TO_VIDEO", "Sora 视频生成"),

    /** 无影厂商。 */
    WUYIN("WUYIN", "无影云生成"),

    /** Runway 平台。 */
    RUNWAY("RUNWAY", "Runway 平台"),

    /** ComfyUI 基础工作流。 */
    COMFY_UI("COMFY_UI", "ComfyUI 基础工作流"),

    /** ComfyUI 图像编辑工作流。 */
    COMFY_UI_IMAGE_EDIT("COMFY_UI_IMAGE_EDIT", "ComfyUI 图像编辑");

    private final String code;
    private final String description;

    VideoProvider(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据编码获取枚举值。
     */
    public static VideoProvider of(String code) {
        if (code == null) {
            return null;
        }
        for (VideoProvider provider : values()) {
            if (provider.code.equals(code)) {
                return provider;
            }
        }
        return null;
    }
}
