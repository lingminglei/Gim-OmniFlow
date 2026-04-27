//package org.lml.config;
//
//import cn.dev33.satoken.interceptor.SaInterceptor;
//import cn.dev33.satoken.stp.StpUtil;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
//import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
//
//@Configuration
//@Slf4j
//public class SaTokenConfigure implements WebMvcConfigurer {
//    // 注册拦截器
//    @Override
//    public void addInterceptors(InterceptorRegistry registry) {
//        System.out.println("✅ 注册 Sa-Token 拦截器");
//        registry.addInterceptor(new SaInterceptor(handler -> {
//                    log.info("🔥 进入 Sa-Token 登录拦截逻辑,");
//                    // TODO: 这里只做登录认证拦截，后续可扩展角色权限处理
//                    StpUtil.checkLogin();
//                }))
//                .addPathPatterns("/**")
//                .excludePathPatterns(
//                        "/**",
//                        "/commonApi/captcha",//获取验证码
//                        "/user/doLogin",//登录操作
//                        "/filetransfer/preview",//文件预览
//                        "/ai/video/createPrompt",
//                        "/ai/video/createVideo",
//                        "/ai/video/queryResult",
//                        "/api/**",
//                        "/chat/aiAsk",
//                        "/api/queryState",
//                        "/user/logout",
//                        "/upload/**",
//                        "/file/upload");
//    }
//}
