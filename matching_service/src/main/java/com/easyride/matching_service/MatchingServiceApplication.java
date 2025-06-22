package com.easyride.matching_service;

import com.easyride.matching_service.config.MatchingConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;


@SpringBootApplication
@EnableConfigurationProperties(MatchingConfig.class)
public class MatchingServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(MatchingServiceApplication.class, args);
	}

}
