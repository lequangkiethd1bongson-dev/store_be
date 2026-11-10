package com.example.storebe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
        "com.example.storebe",
        "vn.team4.store"
})
@EntityScan(basePackages = {"vn.team4.store.model", "vn.team4.store.supplier.entity"})
@EnableJpaRepositories(basePackages = {"vn.team4.store.repository", "vn.team4.store.supplier.repository"})
public class StoreBeApplication {

    public static void main(String[] args) {
        SpringApplication.run(StoreBeApplication.class, args);
    }
}
