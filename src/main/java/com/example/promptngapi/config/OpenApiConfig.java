package com.example.promptngapi.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Arrays;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Prompt NG API",
        version = "v1",
        description = "このAPIは、入力されたテキストプロンプトを分析し、機密情報や潜在的なプロンプトインジェクション攻撃の試みを検出します。"
    )
)
public class OpenApiConfig {

    @Bean
    public GroupedOpenApi publicApi(Environment environment) {
        String[] activeProfiles = environment.getActiveProfiles();
        boolean isProduction = Arrays.asList(activeProfiles).contains("production");

        if (isProduction) {
            // For production, return a GroupedOpenApi that does not expose any paths
            return GroupedOpenApi.builder()
                    .group("prompt-ng-api-prod")
                    .pathsToExclude("/**") // Exclude all paths
                    .build();
        } else {
            // For development or no specific profile, expose all paths
            return GroupedOpenApi.builder()
                    .group("prompt-ng-api-dev")
                    .pathsToMatch("/**") // Include all paths
                    .build();
        }
    }
}
