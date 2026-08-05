package com.example.rcsmock.sim;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

// 설비 자동 시뮬레이션용 스케줄러 — START/DONE 지연 전송에 사용
@Configuration
public class SchedulerConfig {

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        // 여러 팔렛의 START/DONE 이 겹칠 수 있으므로 단일 스레드로 두지 않는다
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("eqp-sim-");
        scheduler.setWaitForTasksToCompleteOnShutdown(false);
        scheduler.initialize();
        return scheduler;
    }
}
