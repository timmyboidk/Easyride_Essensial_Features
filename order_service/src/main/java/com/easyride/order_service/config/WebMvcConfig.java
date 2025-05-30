package com.easyride.order_service.config;

import com.easyride.order_service.interceptor.SignatureVerificationInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

/**
 * Web MVC configuration to register the SignatureVerificationInterceptor
 * so that every incoming HTTP request is checked for signature validity
 * and duplicate submission prevention.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final SignatureVerificationInterceptor signatureVerificationInterceptor;

    @Autowired
    public WebMvcConfig(SignatureVerificationInterceptor signatureVerificationInterceptor) {
        this.signatureVerificationInterceptor = signatureVerificationInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Register the interceptor for all endpoints.
        registry.addInterceptor(signatureVerificationInterceptor).addPathPatterns("/**");
    }
}
