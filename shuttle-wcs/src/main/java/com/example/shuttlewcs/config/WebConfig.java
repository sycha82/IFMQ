package com.example.shuttlewcs.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// 모니터링 대시보드 경로 매핑 — /monitor → static/monitor.html
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/monitor").setViewName("forward:/monitor.html");
    }
}
