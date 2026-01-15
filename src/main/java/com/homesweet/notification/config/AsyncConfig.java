package com.homesweet.notification.config;

import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 비동기 처리 설정
 * 
 * 알림 서비스의 비동기 처리를 위한 스레드 풀 설정
 * 
 * @author dogyungkim
 */
@Slf4j
@Configuration
@EnableAsync
@Profile("!test")
public class AsyncConfig {

    /**
     * 알림 전송 전용 스레드 풀 (Virtual Threads)
     */
    @Bean(name = "notificationTaskExecutor")
    public Executor notificationTaskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("notification-vt-");
        executor.setVirtualThreads(true);
        return executor;
    }
}
