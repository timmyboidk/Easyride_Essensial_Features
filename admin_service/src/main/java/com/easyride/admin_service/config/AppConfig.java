package com.easyride.admin_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@org.springframework.boot.context.properties.EnableConfigurationProperties({
        AdminServiceConfigProperties.ServiceUrls.class,
        AdminServiceConfigProperties.EasyRide.class,
        AdminServiceConfigProperties.RocketMqTopic.class
})
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}