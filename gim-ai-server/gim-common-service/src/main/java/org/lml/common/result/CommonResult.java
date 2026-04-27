package org.lml.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import org.lml.common.enums.ResultStatus;

import java.io.Serializable;

@Data
@AllArgsConstructor
@Getter
public class CommonResult<T> implements Serializable {

    private final Integer code;   // 状态码

    private final T data;   // 返回的数据

    private final String message;    // 自定义信息


    /**
     * 成功的结果
     *
     * @param data 返回结果
     * @param message  返回信息
     */
    public static <T> CommonResult<T> successResponse(T data, String message) {
        return new CommonResult<>(ResultStatus.SUCCESS.getStatus(), data, message);
    }


    /**
     * 成功的结果
     *
     * @param data 返回结果
     */
    public static <T> CommonResult<T> successResponse(T data) {
        return new CommonResult<T>(ResultStatus.SUCCESS.getStatus(), data, "success");
    }

    /**
     * 成功的结果
     *
     * @param message 返回信息
     */
    public static <T> CommonResult<T> successResponse(String message) {
        return new CommonResult<T>(ResultStatus.SUCCESS.getStatus(), null, message);
    }

    /**
     * 成功的结果
     */
    public static <T> CommonResult<T> successResponse() {
        return new CommonResult<T>(ResultStatus.SUCCESS.getStatus(), null, "success");
    }


    /**
     * 失败的结果，无异常
     *
     * @param message 返回信息
     */
    public static <T> CommonResult<T> errorResponse(String message) {
        return new CommonResult<T>(ResultStatus.FAIL.getStatus(), null, message);
    }

    public static <T> CommonResult<T> errorResponse(ResultStatus resultStatus) {
        return new CommonResult<T>(resultStatus.getStatus(), null, resultStatus.getDescription());
    }

    public static <T> CommonResult<T> errorResponse(String message, ResultStatus resultStatus) {
        return new CommonResult<T>(resultStatus.getStatus(), null, message);
    }

    public static <T> CommonResult<T> errorResponse(String message, Integer code) {
        return new CommonResult<T>(code, null, message);
    }

}
