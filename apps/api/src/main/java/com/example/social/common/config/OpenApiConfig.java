package com.example.social.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI socialMediaOpenApi() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("Simple Social Media API")
                                .version("0.1.0")
                                .description("Scaffold API for the Java backend assignment.")
                );
    }
}
