package com.example.storebe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "com.example.storebe",
        "vn.team4.store"
})
public class StoreBeApplication {

    public static void main(String[] args) {
        SpringApplication.run(StoreBeApplication.class, args);
    }
}