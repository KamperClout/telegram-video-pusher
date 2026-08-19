package com.example.telegramvideo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TelegramVideoApplication {

    public static void main(String[] args) {
        SpringApplication.run(TelegramVideoApplication.class, args);
    }
}
