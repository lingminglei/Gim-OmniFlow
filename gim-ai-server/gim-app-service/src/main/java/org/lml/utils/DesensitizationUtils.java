package org.lml.utils;

import cn.hutool.core.util.DesensitizedUtil;
import org.lml.config.sensitive.ZdySensitiveUtils;

/**
 * 日志脱敏 工具包
 */
public class DesensitizationUtils {

    public static String maskData(String data, String type) {
        if (data == null) {
            return null;
        }

        String result;
        switch (type) {
            case "name":
                result = DesensitizedUtil.chineseName(data);
                break;
            case "idCard":
                result = DesensitizedUtil.idCardNum(data, 6, 2);
                break;
            case "phone":
                result = DesensitizedUtil.mobilePhone(data);
                break;
            case "email":
                result = DesensitizedUtil.email(data);
                break;
            case "bankCard":
                result = DesensitizedUtil.bankCard(data);
                break;
            case "address":
                result = DesensitizedUtil.address(data, data.length() - 6);
                break;
            case "patientNo":
                result = ZdySensitiveUtils.patientNo(data);
                break;
            default:
                result = data;
                break;
        }
        return result;

    }

    public static void main(String[] args) {

        String data = "411326200306246956";
        String result = DesensitizedUtil.idCardNum(data, 2, data.length() - 2);

        System.out.println(data.length()-2);
        String resultx = DesensitizedUtil.idCardNum("411326200306246956", 2, data.length()-2);

        System.out.println(result);

        System.out.println(resultx);

    }

}
