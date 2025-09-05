package com.example.gsm.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);        // số core thread luôn giữ
        executor.setMaxPoolSize(10);        // số thread tối đa
        executor.setQueueCapacity(100);     // hàng đợi
        executor.setThreadNamePrefix("Async-"); // prefix tên thread
        executor.initialize();
        return executor;
    }
}
