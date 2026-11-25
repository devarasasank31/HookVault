package com.hookvault;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HookVaultApplication {

    public static void main(String[] args) {
        SpringApplication.run(HookVaultApplication.class, args);
    }
}