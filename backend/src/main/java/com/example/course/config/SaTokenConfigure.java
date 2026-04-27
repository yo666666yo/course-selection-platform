package com.example.course.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册 Sa-Token 拦截器
        registry.addInterceptor(new SaInterceptor(handler -> {
            // 指定拦截路由与排除路由
            SaRouter.match("/**")
                    .notMatch("/login", "/user/register", "/error") // 排除登录等接口
                    .notMatch("/*.ico", "/*.png", "/*.jpg", "/*.css", "/*.js") // 排除静态资源
                    .check(r -> {
                        // ✨✨✨ 关键修复：使用 SaHolder.getRequest() 获取请求方式 ✨✨✨
                        // 之前的 r.getMethod() 是错误的写法
                        if ("OPTIONS".equalsIgnoreCase(SaHolder.getRequest().getMethod())) {
                            return;
                        }
                        // 其他请求必须登录
                        StpUtil.checkLogin();
                    });
        })).addPathPatterns("/**");
    }
}