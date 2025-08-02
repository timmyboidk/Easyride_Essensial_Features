package com.easyride.payment_service.repository;

import com.easyride.payment_service.model.PassengerPaymentMethod;
import com.easyride.payment_service.model.PaymentMethodType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("乘客支付方式仓库 (PassengerPaymentMethodRepository) 测试")
class PassengerPaymentMethodRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PassengerPaymentMethodRepository paymentMethodRepository;

    private PassengerPaymentMethod method1;
    private PassengerPaymentMethod method2_default;

    @BeforeEach
    void setUp() {
        // 准备属于乘客1的两条支付记录
        method1 = new PassengerPaymentMethod();
        method1.setPassengerId(1L);
        method1.setMethodType(PaymentMethodType.CREDIT_CARD);
        method1.setPaymentGatewayToken("token_1");
        method1.setDefault(false);

        method2_default = new PassengerPaymentMethod();
        method2_default.setPassengerId(1L);
        method2_default.setMethodType(PaymentMethodType.PAYPAL);
        method2_default.setPaymentGatewayToken("token_2");
        method2_default.setDefault(true); // 设为默认

        // 准备属于乘客2的一条支付记录
        PassengerPaymentMethod method3 = new PassengerPaymentMethod();
        method3.setPassengerId(2L);
        method3.setMethodType(PaymentMethodType.CREDIT_CARD);
        method3.setPaymentGatewayToken("token_3");
        method3.setDefault(true);

        // 持久化这些数据
        entityManager.persist(method1);
        entityManager.persist(method2_default);
        entityManager.persist(method3);
        entityManager.flush();
    }

    @Test
    @DisplayName("findByPassengerId 应返回该乘客的所有支付方式")
    void findByPassengerId_shouldReturnAllMethodsForPassenger() {
        List<PassengerPaymentMethod> methods = paymentMethodRepository.findByPassengerId(1L);
        assertThat(methods).hasSize(2).containsExactlyInAnyOrder(method1, method2_default);
    }

    @Test
    @DisplayName("findByPassengerIdAndIsDefaultTrue 应返回该乘客的默认支付方式")
    void findByPassengerIdAndIsDefaultTrue_shouldReturnDefaultMethod() {
        Optional<PassengerPaymentMethod> defaultMethod = paymentMethodRepository.findByPassengerIdAndIsDefaultTrue(1L);
        assertThat(defaultMethod).isPresent();
        assertThat(defaultMethod.get().getId()).isEqualTo(method2_default.getId());
        assertThat(defaultMethod.get().isDefault()).isTrue();
    }

    @Test
    @DisplayName("findByPaymentGatewayToken 应能根据Token返回唯一的支付方式")
    void findByPaymentGatewayToken_shouldReturnMethodForToken() {
        Optional<PassengerPaymentMethod> foundMethod = paymentMethodRepository.findByPaymentGatewayToken("token_1");
        assertThat(foundMethod).isPresent();
        assertThat(foundMethod.get().getId()).isEqualTo(method1.getId());
    }

    @Test
    @DisplayName("findByIdAndPassengerId 应在ID和乘客ID都匹配时返回记录")
    void findByIdAndPassengerId_shouldReturnMethod_whenIdsMatch() {
        Optional<PassengerPaymentMethod> foundMethod = paymentMethodRepository.findByIdAndPassengerId(method1.getId(), 1L);
        assertThat(foundMethod).isPresent();

        // 验证ID不匹配但乘客ID匹配时找不到
        Optional<PassengerPaymentMethod> notFoundById = paymentMethodRepository.findByIdAndPassengerId(999L, 1L);
        assertThat(notFoundById).isNotPresent();

        // 验证ID匹配但乘客ID不匹配时找不到
        Optional<PassengerPaymentMethod> notFoundByPassenger = paymentMethodRepository.findByIdAndPassengerId(method1.getId(), 2L);
        assertThat(notFoundByPassenger).isNotPresent();
    }
}