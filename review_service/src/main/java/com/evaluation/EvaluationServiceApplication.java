package com.evaluation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springdoc.core.models.GroupedOpenApi;

@SpringBootApplication
@EnableFeignClients
public class EvaluationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EvaluationServiceApplication.class, args);
    }

    @Bean
    public GroupedOpenApi evaluationApi() {
        return GroupedOpenApi.builder()
                .group("evaluation-service")
                .pathsToMatch("/api/evaluations/**", "/api/complaints/**")
                .build();
    }
}
