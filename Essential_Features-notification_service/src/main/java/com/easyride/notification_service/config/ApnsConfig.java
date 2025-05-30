package com.easyride.notification_service.config;

import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.ApnsClientBuilder;
import com.eatthepath.pushy.apns.auth.ApnsSigningKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Configuration
public class ApnsConfig {

    @Value("${apns.authKey.path}")
    private String authKeyPath;

    @Value("${apns.teamId}")
    private String teamId;

    @Value("${apns.keyId}")
    private String keyId;

    @Value("${apns.environment}")
    private String environment;

    @Bean
    public ApnsClient apnsClient() throws Exception {
        String apnsServer = environment.equalsIgnoreCase("production")
                ? ApnsClientBuilder.PRODUCTION_APNS_HOST
                : ApnsClientBuilder.DEVELOPMENT_APNS_HOST;

        return new ApnsClientBuilder()
                .setApnsServer(apnsServer)
                .setSigningKey(ApnsSigningKey.loadFromPkcs8File(
                        new File(authKeyPath), teamId, keyId))
                .build();
    }
}
