package com.citybus.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class CityBusPlatformApplication {
    public static void main(String[] args) {
        SpringApplication.run(CityBusPlatformApplication.class, args);
    }
}
