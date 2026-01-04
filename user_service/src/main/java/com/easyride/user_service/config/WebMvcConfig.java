package com.easyride.user_service.config;

import com.easyride.user_service.interceptor.SignatureVerificationInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final SignatureVerificationInterceptor signatureVerificationInterceptor;

    public WebMvcConfig(SignatureVerificationInterceptor signatureVerificationInterceptor) {
        this.signatureVerificationInterceptor = signatureVerificationInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 对所有接口生效，可根据实际需求调整路径
        registry.addInterceptor(signatureVerificationInterceptor)
                .addPathPatterns("/**") // 拦截所有路径...
                .excludePathPatterns("/users/register", "/users/login", "/users/otp/**", "/users/login/otp"); // ...除了这些公共端点
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:3000", "https://ER-frontend-domain.com") // <-- 明确指定前端来源
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
