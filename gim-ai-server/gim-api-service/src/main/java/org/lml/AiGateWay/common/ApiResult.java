package org.lml.AiGateWay.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.lml.AiGateWay.enums.GatewayTaskStatus;

import java.io.Serializable;

/**
 * AI 网关统一响应对象。
 *
 * @param <T> 响应数据类型
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApiResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 接口调用是否成功。 */
    private boolean success;

    /** 业务状态码。 */
    private String code;

    /** 业务提示信息。 */
    private String msg;

    /** 网关任务状态。 */
    private GatewayTaskStatus status;

    /** 业务返回数据。 */
    private T data;

    /**
     * 返回处理中结果。
     */
    public static <T> ApiResult<T> processing(T data, String msg) {
        return ApiResult.<T>builder()
                .success(true)
                .status(GatewayTaskStatus.PROCESSING)
                .code("202")
                .data(data)
                .msg(msg != null ? msg : "任务正在处理中")
                .build();
    }

    /**
     * 返回成功结果。
     */
    public static <T> ApiResult<T> success(T data) {
        return ApiResult.<T>builder()
                .success(true)
                .status(GatewayTaskStatus.SUCCESS)
                .code("200")
                .data(data)
                .msg("处理成功")
                .build();
    }

    /**
     * 返回业务失败结果。
     */
    public static <T> ApiResult<T> taskFail(String msg) {
        return ApiResult.<T>builder()
                .success(true)
                .status(GatewayTaskStatus.FAIL)
                .code("500")
                .msg(msg)
                .build();
    }

    /**
     * 返回系统异常结果。
     */
    public static <T> ApiResult<T> error(String code, String msg) {
        return ApiResult.<T>builder()
                .success(false)
                .status(GatewayTaskStatus.FAIL)
                .code(code)
                .msg(msg)
                .build();
    }
}
