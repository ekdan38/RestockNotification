package com.example.restocknotification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class RestockNotificationApplication {

    public static void main(String[] args) {
        SpringApplication.run(RestockNotificationApplication.class, args);
    }

}
