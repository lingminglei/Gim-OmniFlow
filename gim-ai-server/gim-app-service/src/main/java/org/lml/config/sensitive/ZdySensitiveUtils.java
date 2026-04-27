package org.lml.config.sensitive;

/**
 * 自定义脱敏规则
 */
public class ZdySensitiveUtils {

    /**
     * 对患者号进行脱敏
     */
    public static String patientNo(String patientNo) {
        if (patientNo == null || patientNo.length() < 4) {
            return patientNo;
        }
        return patientNo.substring(0, 4) + "****" + patientNo.substring(patientNo.length() - 2);
    }
}
