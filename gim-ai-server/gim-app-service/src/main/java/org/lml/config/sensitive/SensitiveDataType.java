package org.lml.config.sensitive;

import lombok.Getter;

/**
 * 脱敏枚举类
 */
public enum SensitiveDataType {
    //脱敏数据类型
    /**
     * 姓名
     */
    NAME("name"),
    /**
     * 身份证号
     */
    ID_CARD("idCard"),
    /**
     * 手机号
     */
    PHONE("phone"),
    /**
     * 邮箱
     */
    EMAIL("email"),
    /**
     * 银行卡号
     */
    BANK_CARD("bankCard"),
    /**
     * 地址
     */
    ADDRESS("address"),
    /**
     * 密码
     */
    PASSWORD("password"),

    /**
     * 患者号
     */
    PATIENT_NO("patientNo"),
    ;

    SensitiveDataType(String type) {
        this.type = type;
    }
    @Getter
    private String type;
}
