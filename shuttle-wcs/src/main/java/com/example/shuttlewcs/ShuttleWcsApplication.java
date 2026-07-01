package com.example.shuttlewcs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class ShuttleWcsApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShuttleWcsApplication.class, args);
    }
}
