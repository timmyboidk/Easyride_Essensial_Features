package com.easyride.user_service.config;

import com.easyride.user_service.interceptor.SignatureVerificationInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final SignatureVerificationInterceptor signatureVerificationInterceptor;

    @Autowired
    public WebMvcConfig(SignatureVerificationInterceptor signatureVerificationInterceptor) {
        this.signatureVerificationInterceptor = signatureVerificationInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 对所有接口生效，可根据实际需求调整路径
        registry.addInterceptor(signatureVerificationInterceptor).addPathPatterns("/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")  // 或指定前端应用域名
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
