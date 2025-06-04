package com.example.promptngapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties; // Added import
import com.example.promptngapi.config.ScoreThresholdsConfig; // Added import

@SpringBootApplication
@EnableConfigurationProperties(ScoreThresholdsConfig.class) // Added annotation
public class PromptNgApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(PromptNgApiApplication.class, args);
    }

}
