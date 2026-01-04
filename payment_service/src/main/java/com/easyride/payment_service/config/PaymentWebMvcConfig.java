package com.easyride.payment_service.config;

import com.easyride.payment_service.interceptor.PaymentSignatureVerificationInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class PaymentWebMvcConfig implements WebMvcConfigurer {

    private final PaymentSignatureVerificationInterceptor signatureInterceptor;

    public PaymentWebMvcConfig(PaymentSignatureVerificationInterceptor signatureInterceptor) {
        this.signatureInterceptor = signatureInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 对支付相关接口生效，可根据实际需求调整匹配路径
        registry.addInterceptor(signatureInterceptor).addPathPatterns("/payments/**");
    }
}
