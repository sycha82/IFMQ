package com.example.wcsapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class WcsAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(WcsAppApplication.class, args);
    }
}
