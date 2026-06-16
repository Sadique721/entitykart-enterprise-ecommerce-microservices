package com.entitykart.wishlistservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@ComponentScan(basePackages = {"com.entitykart.wishlistservice", "com.entitykart.common"})
public class WishlistServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(WishlistServiceApplication.class, args);
        System.out.println("✅✅✅ APPLICATION STARTED SUCCESSFULLY ✅✅✅");
    }
}
