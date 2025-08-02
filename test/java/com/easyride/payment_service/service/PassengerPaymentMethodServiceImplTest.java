package com.easyride.payment_service.service;

import com.easyride.payment_service.dto.AddPaymentMethodRequestDto;
import com.easyride.payment_service.dto.PaymentMethodResponseDto;
import com.easyride.payment_service.exception.PaymentServiceException;
import com.easyride.payment_service.exception.ResourceNotFoundException;
import com.easyride.payment_service.model.PassengerPaymentMethod;
import com.easyride.payment_service.model.PaymentMethodType;
import com.easyride.payment_service.repository.PassengerPaymentMethodRepository;
import com.easyride.payment_service.util.PaymentGatewayUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("乘客支付方式服务 (PassengerPaymentMethodServiceImpl) 测试")
class PassengerPaymentMethodServiceImplTest {

    @Mock
    private PassengerPaymentMethodRepository paymentMethodRepository;

    @Mock
    private PaymentGatewayUtil paymentGatewayUtil;

    @InjectMocks
    private PassengerPaymentMethodServiceImpl passengerPaymentMethodService;

    private AddPaymentMethodRequestDto addRequest;
    private PaymentGatewayUtil.GatewayProcessedPaymentMethod processedMethod;

    @BeforeEach
    void setUp() {
        // 准备一个标准的添加支付方式的请求 DTO
        addRequest = new AddPaymentMethodRequestDto();
        addRequest.setMethodType(PaymentMethodType.CREDIT_CARD);
        addRequest.setPaymentGatewayNonce("nonce_12345"); // 一次性令牌
        addRequest.setDefault(true);

        // 准备一个由 PaymentGatewayUtil 处理后返回的模拟对象
        processedMethod = PaymentGatewayUtil.GatewayProcessedPaymentMethod.builder()
                .permanentToken("perm_token_abcde") // 永久令牌
                .gatewayCustomerId("cust_1")
                .cardLastFour("4242")
                .cardBrand("Visa")
                .build();
    }

    // 将此方法添加到 PassengerPaymentMethodServiceImplTest 类中

    @Test
    @DisplayName("首次添加支付方式时，应自动设为默认")
    void addPaymentMethod_shouldSetAsDefault_whenItIsTheFirstMethod() {
        // --- Given (安排/准备) ---
        addRequest.setDefault(false); // 明确请求不设为默认

        // 1. 模拟网关处理 nonce 成功
        when(paymentGatewayUtil.processAndStorePaymentMethodNonce(anyLong(), anyString(), any()))
                .thenReturn(processedMethod);

        // 2. 模拟这是一个新令牌，数据库中不存在
        when(paymentMethodRepository.findByPaymentGatewayToken("perm_token_abcde"))
                .thenReturn(Optional.empty());

        // 3. 关键：模拟该用户当前没有任何支付方式
        when(paymentMethodRepository.findByPassengerId(anyLong())).thenReturn(Collections.emptyList());

        // 4. 模拟 save 操作
        when(paymentMethodRepository.save(any(PassengerPaymentMethod.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // --- When (执行) ---
        passengerPaymentMethodService.addPaymentMethod(1L, addRequest);

        // --- Then (断言/验证) ---
        // 验证保存到数据库的对象中 isDefault 字段为 true
        verify(paymentMethodRepository).save(argThat(savedMethod ->
                savedMethod.isDefault() // 验证它被设为了默认
        ));
    }

    // 将此方法添加到 PassengerPaymentMethodServiceImplTest 类中

    @Test
    @DisplayName("添加新的默认支付方式时，应取消之前的默认支付方式")
    void addPaymentMethod_shouldUnsetOldDefault_whenAddingNewDefault() {
        // --- Given ---
        addRequest.setDefault(true); // 明确请求设为默认

        // 准备一个已存在的、旧的默认支付方式
        PassengerPaymentMethod oldDefault = new PassengerPaymentMethod();
        oldDefault.setId(88L);
        oldDefault.setDefault(true);

        when(paymentGatewayUtil.processAndStorePaymentMethodNonce(anyLong(), anyString(), any()))
                .thenReturn(processedMethod);
        when(paymentMethodRepository.findByPaymentGatewayToken(anyString()))
                .thenReturn(Optional.empty());

        // 关键：模拟用户已有一个默认支付方式
        when(paymentMethodRepository.findByPassengerIdAndIsDefaultTrue(1L))
                .thenReturn(Optional.of(oldDefault));

        // 模拟 save 操作
        when(paymentMethodRepository.save(any(PassengerPaymentMethod.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // --- When ---
        passengerPaymentMethodService.addPaymentMethod(1L, addRequest);

        // --- Then ---
        // 验证 save 方法被调用了两次
        // 第一次是保存旧的默认方法（isDefault 变为 false）
        // 第二次是保存新的默认方法（isDefault 为 true）
        verify(paymentMethodRepository, times(2)).save(any(PassengerPaymentMethod.class));

        // 验证旧的默认支付方式被更新为了非默认
        verify(paymentMethodRepository).save(argThat(method ->
                method.getId() != null && method.getId().equals(88L) && !method.isDefault()
        ));
        // 验证新添加的支付方式被设为了默认
        verify(paymentMethodRepository).save(argThat(method ->
                method.getId() == null && method.isDefault() // 新方法ID为null
        ));
    }

    // 将此方法添加到 PassengerPaymentMethodServiceImplTest 类中

    @Test
    @DisplayName("当尝试添加一个已存在的支付方式（相同token）时，应抛出异常")
    void addPaymentMethod_shouldThrowException_whenTokenExists() {
        // --- Given ---
        when(paymentGatewayUtil.processAndStorePaymentMethodNonce(anyLong(), anyString(), any()))
                .thenReturn(processedMethod);

        // 关键：模拟数据库中已经存在这个永久令牌
        when(paymentMethodRepository.findByPaymentGatewayToken("perm_token_abcde"))
                .thenReturn(Optional.of(new PassengerPaymentMethod()));

        // --- When & Then ---
        assertThatThrownBy(() -> {
            passengerPaymentMethodService.addPaymentMethod(1L, addRequest);
        })
                .isInstanceOf(PaymentServiceException.class)
                .hasMessage("此支付方式似乎已添加。");
    }

    // 将此方法添加到 PassengerPaymentMethodServiceImplTest 类中

    @Test
    @DisplayName("当删除默认支付方式时，应将另一张卡设为新的默认")
    void deletePaymentMethod_shouldSetNewDefault_whenDeletingDefault() {
        // --- Given ---
        // 准备两条支付记录，一个是默认的，一个不是
        PassengerPaymentMethod methodToDelete = new PassengerPaymentMethod();
        methodToDelete.setId(1L);
        methodToDelete.setPassengerId(1L);
        methodToDelete.setDefault(true); // 这是要被删除的默认卡

        PassengerPaymentMethod newDefaultMethod = new PassengerPaymentMethod();
        newDefaultMethod.setId(2L);
        newDefaultMethod.setPassengerId(1L);
        newDefaultMethod.setDefault(false); // 这张卡将成为新的默认

        // 模拟根据 ID 和用户 ID 能找到要删除的卡
        when(paymentMethodRepository.findByIdAndPassengerId(1L, 1L))
                .thenReturn(Optional.of(methodToDelete));

        // 关键：模拟删除之后，当查询该用户的所有支付方式时，返回剩下的那张卡
        when(paymentMethodRepository.findByPassengerId(1L)).thenReturn(List.of(newDefaultMethod));

        // --- When ---
        passengerPaymentMethodService.deletePaymentMethod(1L, 1L);

        // --- Then ---
        // 验证 delete 方法确实被调用了
        verify(paymentMethodRepository).delete(methodToDelete);

        // 验证 save 方法被调用，用来更新新的默认卡
        verify(paymentMethodRepository).save(argThat(savedMethod ->
                savedMethod.getId().equals(2L) && savedMethod.isDefault()
        ));
    }

    @Test
    @DisplayName("当设置的默认支付方式不存在时，应抛出 ResourceNotFoundException")
    void setDefaultPaymentMethod_shouldThrowException_whenMethodNotFound() {
        // --- Given ---
        // 模拟根据 ID 找不到任何支付方式
        when(paymentMethodRepository.findByIdAndPassengerId(anyLong(), anyLong()))
                .thenReturn(Optional.empty());

        // --- When & Then ---
        assertThatThrownBy(() -> {
            passengerPaymentMethodService.setDefaultPaymentMethod(1L, 999L);
        })
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("支付方式未找到或不属于该用户");
    }
}