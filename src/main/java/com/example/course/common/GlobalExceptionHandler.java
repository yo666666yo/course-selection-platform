package com.example.course.common;

import cn.dev33.satoken.exception.NotLoginException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 捕获 Sa-Token 的未登录异常
     * 强制返回 HTTP 401 状态码，配合前端 Axios 拦截器跳转登录页
     */
    @ExceptionHandler(NotLoginException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED) // ✨ 设置 HTTP 状态码为 401
    public Result<String> handlerNotLoginException(NotLoginException nle) {
        // 打印一点日志方便调试，生产环境可关闭
        System.out.println("鉴权失败: " + nle.getMessage());
        return Result.error(401, "您的登录已过期，请重新登录");
    }

    /**
     * 捕获其他通用异常
     */
    @ExceptionHandler(Exception.class)
    public Result<String> handlerException(Exception e) {
        e.printStackTrace(); // 打印堆栈
        return Result.error(500, e.getMessage());
    }
}