package com.example.course.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 启用异步支持。
 * 事件监听器标注 @Async 后将在独立线程池执行，不阻塞主业务流程。
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
