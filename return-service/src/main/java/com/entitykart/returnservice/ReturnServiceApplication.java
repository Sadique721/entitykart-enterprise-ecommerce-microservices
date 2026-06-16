package com.entitykart.returnservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class ReturnServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReturnServiceApplication.class, args);
        System.out.println("✅✅✅ APPLICATION STARTED SUCCESSFULLY ✅✅✅");
    }
}
