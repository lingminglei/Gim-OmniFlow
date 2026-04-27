package org.lml.common.enums;

/**
 * 公共枚举
 */
public enum CommonEnum {

    /**
     * 默认枚举
     */
    COMMON_MOREN("默认枚举"),

    /**
     * 用户在线状态：离线
     */
    USER_INFO_OFFLINE_STATUS("offline"),

    /**
     * 用户在线状态：在线
     */
    USER_INFO_ONLINE_STATUS("online"),

    ;

    // 枚举构造函数，用于设置枚举项的值
    private String value;

    CommonEnum(String value) {
        this.value = value;
    }

    // 获取枚举项的值
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
