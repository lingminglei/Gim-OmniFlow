package org.lml.config;

import cn.dev33.satoken.exception.NotLoginException;
import lombok.extern.slf4j.Slf4j;
import org.lml.common.enums.ResultStatus;
import org.lml.common.exection.BizException;
import org.lml.common.exection.TokenValidationException;
import org.lml.common.result.CommonResult;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * 全局异常处理。
 */
@Slf4j(topic = "lml")
@RestControllerAdvice
public class GlobalExceptionAdvice {

    /**
     * 处理参数校验异常。
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public CommonResult<Void> handlerMethodArgumentNotValidException(MethodArgumentNotValidException e,
                                                                     HttpServletRequest request) {
        BindingResult bindingResult = e.getBindingResult();
        ObjectError objectError = bindingResult.getAllErrors().stream().findFirst().orElse(null);
        String message = objectError == null ? "请求参数不合法" : objectError.getDefaultMessage();
        log.warn("参数校验失败，请求路径={}, 请求方式={}, token摘要={}, 异常信息={}",
                request.getRequestURI(),
                request.getMethod(),
                getTokenSummary(request),
                message);
        return CommonResult.errorResponse(message, ResultStatus.FAIL);
    }

    /**
     * 处理 token 校验异常。
     */
    @ExceptionHandler(TokenValidationException.class)
    public CommonResult<Void> handleTokenValidationException(TokenValidationException ex,
                                                             HttpServletRequest request) {
        log.warn("Token 校验失败，请求标识={}, 请求路径={}, 请求方式={}, token摘要={}, 异常类型={}, 异常信息={}",
                getRequestId(request),
                request.getRequestURI(),
                request.getMethod(),
                getTokenSummary(request),
                ex.getClass().getSimpleName(),
                ex.getMessage());
        return CommonResult.errorResponse(ex.getMessage(), ResultStatus.ACCESS_DENIED);
    }

    /**
     * 处理请求类型不符的问题
     * @param e
     * @return
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public CommonResult<?> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.error("不支持的请求方式: {}", e.getMethod());
        return CommonResult.errorResponse("请求方式不支持，请使用 " + e.getSupportedHttpMethods(),405);
    }

    /**
     * 处理未登录异常。
     */
    @ExceptionHandler(NotLoginException.class)
    public CommonResult<Void> handleNotLoginException(NotLoginException e,
                                                      HttpServletRequest request) {
        log.warn("用户未登录，请求标识={}, 请求路径={}, 请求方式={}, token摘要={}, 异常类型={}, 异常信息={}",
                getRequestId(request),
                request.getRequestURI(),
                request.getMethod(),
                getTokenSummary(request),
                e.getClass().getSimpleName(),
                e.getMessage());
        return CommonResult.errorResponse("用户未登录", ResultStatus.ACCESS_DENIED);
    }

    /**
     * 处理业务异常。
     */
    @ExceptionHandler(BizException.class)
    public CommonResult<Void> handleBizException(BizException e,
                                                 HttpServletRequest request) {
        log.error("业务异常，请求标识={}, 请求路径={}, 请求方式={}, token摘要={}, 异常类型={}, 异常信息={}",
                getRequestId(request),
                request.getRequestURI(),
                request.getMethod(),
                getTokenSummary(request),
                e.getClass().getSimpleName(),
                e.getMessage(),
                e);
        return CommonResult.errorResponse(e.getMessage(), ResultStatus.FAIL);
    }

    /**
     * 处理兜底系统异常。
     */
    @ExceptionHandler(Exception.class)
    public CommonResult<Void> handleException(Exception e,
                                              HttpServletRequest request) {
        log.error("系统异常，请求标识={}, 请求路径={}, 请求方式={}, token摘要={}, 异常类型={}, 异常信息={}",
                getRequestId(request),
                request.getRequestURI(),
                request.getMethod(),
                getTokenSummary(request),
                e.getClass().getSimpleName(),
                e.getMessage(),
                e);
        return CommonResult.errorResponse("系统异常", ResultStatus.SYSTEM_ERROR);
    }

    /**
     * 提取请求中的 token 摘要，避免完整 token 进入日志。
     */
    private String getTokenSummary(HttpServletRequest request) {
        String token = request.getHeader("satoken");
        if (token == null || token.trim().isEmpty()) {
            token = request.getParameter("token");
        }
        if (token == null || token.trim().isEmpty()) {
            return "无";
        }
        if (token.length() <= 8) {
            return token;
        }
        return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
    }

    /**
     * 提取网关注入的请求标识。
     */
    private String getRequestId(HttpServletRequest request) {
        String requestId = request.getHeader("X-Request-Id");
        return requestId == null || requestId.trim().isEmpty() ? "无" : requestId;
    }

    /**
     * 打印异常堆栈字符串。
     */
    public static String getMessage(Exception e) {
        String swStr = null;
        try (StringWriter sw = new StringWriter();
             PrintWriter pw = new PrintWriter(sw)) {
            e.printStackTrace(pw);
            pw.flush();
            sw.flush();
            swStr = sw.toString();
        } catch (IOException ex) {
            log.error("异常堆栈打印失败", ex);
        }
        return swStr;
    }
}
