package com.easyride.payment_service.service;

import com.easyride.payment_service.dto.*;
import com.easyride.payment_service.exception.PaymentServiceException;
import com.easyride.payment_service.exception.ResourceNotFoundException;
import com.easyride.payment_service.model.PassengerPaymentMethod;
import com.easyride.payment_service.model.Payment;
import com.easyride.payment_service.model.PaymentStatus;
import com.easyride.payment_service.repository.PassengerPaymentMethodRepository;
import com.easyride.payment_service.repository.PaymentRepository;
import com.easyride.payment_service.rocketmq.PaymentEventProducer;
import com.easyride.payment_service.util.PaymentGatewayUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("支付核心服务 (PaymentServiceImpl) 测试")
class PaymentServiceImplTest {

    // --- 模拟所有依赖 ---
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private WalletService walletService;
    @Mock
    private PaymentGatewayUtil paymentGatewayUtil;
    @Mock
    private StringRedisTemplate redisTemplate; // 尽管在当前代码中没直接使用，但构造函数需要它
    @Mock
    private PaymentEventProducer paymentEventProducer;
    @Mock
    private PaymentStrategyProcessor strategyProcessor;
    @Mock
    private PassengerPaymentMethodRepository passengerPaymentMethodRepository;

    // --- 注入模拟对象并创建被测实例 ---
    @InjectMocks
    private PaymentServiceImpl paymentService;

    // --- 通用测试数据 ---
    private PaymentRequestDto paymentRequest;
    private PassengerPaymentMethod storedPaymentMethod;
    private PaymentResponseDto strategyResponse;

    @BeforeEach
    void setUp() {
        // 准备一个标准的支付请求
        paymentRequest = new PaymentRequestDto();
        paymentRequest.setOrderId(100L);
        paymentRequest.setPassengerId(1L);
        paymentRequest.setAmount(5000); // 50元
        paymentRequest.setCurrency("CNY");
        paymentRequest.setPaymentMethod("CREDIT_CARD");
        paymentRequest.setPaymentMethodId(99L); // 假设使用一个已存储的支付方式

        // 准备一个已存储的支付方式的模拟对象
        storedPaymentMethod = new PassengerPaymentMethod();
        storedPaymentMethod.setId(99L);
        storedPaymentMethod.setPassengerId(1L);
        // ... 其他属性可以按需设置

        // 准备一个由 Strategy 返回的标准成功响应
        strategyResponse = new PaymentResponseDto();
        strategyResponse.setStatus(PaymentStatus.COMPLETED);
        strategyResponse.setTransactionId("txn_123456789");
        strategyResponse.setPaymentGatewayUsed("STRIPE");
        strategyResponse.setMessage("支付成功");
    }
    @Test
    @DisplayName("当支付成功但未找到司机ID时，应只保存记录而不更新钱包")
    void processPayment_shouldSucceed_butNotUpdateWallet_whenDriverIdIsNotFound() {
        // --- Given (安排/准备) ---

        // 1. 模拟找到已存的支付方式
        when(passengerPaymentMethodRepository.findByIdAndPassengerId(99L, 1L))
                .thenReturn(Optional.of(storedPaymentMethod));

        // 2. 模拟支付策略执行成功
        when(strategyProcessor.processPayment(paymentRequest, storedPaymentMethod))
                .thenReturn(strategyResponse);

        // 3. 关键：模拟 getDriverIdByOrderId() 返回 null。
        //    这是通过模拟 findByOrderId 返回一个不含 driverId 的 Payment 对象来实现的。
        Payment paymentWithoutDriver = new Payment();
        paymentWithoutDriver.setDriverId(null);
        when(paymentRepository.findByOrderId(100L)).thenReturn(Optional.of(paymentWithoutDriver));

        // 4. 模拟 save 操作
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        // --- When (执行) ---
        PaymentResponseDto finalResponse = paymentService.processPayment(paymentRequest);

        // --- Then (断言/验证) ---

        // 1. 响应依然是成功的
        assertThat(finalResponse.getStatus()).isEqualTo(PaymentStatus.COMPLETED);

        // 2. 验证 save 方法只被调用了一次！
        //    因为没有 driverId，包含第二次 save 的代码块被跳过了。
        verify(paymentRepository, times(1)).save(any(Payment.class));

        // 3. 验证钱包服务完全没有被调用
        verifyNoInteractions(walletService);

        // 4. 验证成功事件依然被发布
        verify(paymentEventProducer).sendPaymentEvent(any(PaymentEventDto.class));
    }

    @Test
    @DisplayName("支付成功且能找到司机ID时，应给司机钱包加款")
    void processPayment_shouldAddFundsToDriverWallet_whenDriverIdIsFound() {
        // --- Given ---

        // 和上一个测试大部分相同
        when(passengerPaymentMethodRepository.findByIdAndPassengerId(99L, 1L))
                .thenReturn(Optional.of(storedPaymentMethod));
        when(strategyProcessor.processPayment(paymentRequest, storedPaymentMethod))
                .thenReturn(strategyResponse);

        // 关键不同点：模拟一个已经包含了司机ID的Payment对象
        Payment paymentWithDriver = new Payment();
        paymentWithDriver.setDriverId(88L); // 假设司机ID为88L
        // 当 paymentService 内部调用 getDriverIdByOrderId 时，通过 findByOrderId 就能找到这个司机ID
        when(paymentRepository.findByOrderId(100L)).thenReturn(Optional.of(paymentWithDriver));

        // --- When ---
        paymentService.processPayment(paymentRequest);

        // --- Then ---

        // 最核心的验证：验证 addFunds 方法被以正确的司机ID和金额调用了
        verify(walletService).addFunds(88L, 5000);

        // 顺便验证其他交互依然正确
        verify(paymentEventProducer).sendPaymentEvent(any(PaymentEventDto.class));
        verify(paymentRepository, times(2)).save(any(Payment.class));
    }

    // 将此方法添加到 PaymentServiceImplTest 类中

    @Test
    @DisplayName("当支付策略返回 FAILED 时，应发送失败事件且不操作钱包")
    void processPayment_shouldHandleFailure_whenStrategyFails() {
        // --- Given ---

        // 关键：让策略处理器返回一个失败的响应
        strategyResponse.setStatus(PaymentStatus.FAILED);
        strategyResponse.setMessage("银行卡余额不足");

        when(passengerPaymentMethodRepository.findByIdAndPassengerId(99L, 1L))
                .thenReturn(Optional.of(storedPaymentMethod));
        when(strategyProcessor.processPayment(paymentRequest, storedPaymentMethod))
                .thenReturn(strategyResponse);

        // 即使失败，也需要保存一条 FAILED 状态的支付记录
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        // --- When ---
        PaymentResponseDto finalResponse = paymentService.processPayment(paymentRequest);

        // --- Then ---

        // 1. 验证返回的是失败的响应
        assertThat(finalResponse.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(finalResponse.getMessage()).isEqualTo("银行卡余额不足");

        // 2. 验证钱包服务完全没有被调用
        verifyNoInteractions(walletService);

        // 3. 验证发送的是“失败”事件
        verify(paymentEventProducer).sendPaymentFailedEvent(any(PaymentFailedEventDto.class));
        // 验证“成功”事件没有被发送
        verify(paymentEventProducer, never()).sendPaymentEvent(any(PaymentEventDto.class));

        // 4. 验证 paymentRepository 只被调用了1次，用于保存 FAILED 状态的记录
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    @DisplayName("边缘情况：当请求中没有支付方式ID和Nonce时，应抛出异常")
    void processPayment_shouldThrowException_whenNoMethodIdAndNonce() {
        // --- Given ---
        // 这是测试开始的校验逻辑
        paymentRequest.setPaymentMethodId(null);
        paymentRequest.setPaymentGatewayNonce(null);

        // --- When & Then ---
        assertThatThrownBy(() -> {
            paymentService.processPayment(paymentRequest);
        })
                .isInstanceOf(PaymentServiceException.class)
                .hasMessage("有效的支付方式ID或支付网关nonce必须提供。");

        // 确保在校验失败后，后续的逻辑完全不会执行
        verifyNoInteractions(passengerPaymentMethodRepository, strategyProcessor, paymentRepository, walletService, paymentEventProducer);
    }
    @Test
    @DisplayName("当支付状态为 COMPLETED 时，应能成功处理全额退款")
    void refundPayment_shouldSucceedForFullRefund() {
        // --- Given (安排/准备) ---

        // 1. 准备一个已完成支付的 Payment 实体
        Payment completedPayment = new Payment();
        completedPayment.setId(123L);
        completedPayment.setOrderId(100L);
        completedPayment.setDriverId(88L);
        completedPayment.setAmount(5000);
        completedPayment.setRefundedAmount(0); // 初始已退款为0
        completedPayment.setStatus(PaymentStatus.COMPLETED);
        completedPayment.setTransactionId("txn_123456789");
        completedPayment.setPaymentGateway("STRIPE");
        completedPayment.setCurrency("CNY");

        // 2. 准备一个代表退款成功的 PaymentResponseDto
        PaymentResponseDto refundStrategyResponse = new PaymentResponseDto();
        refundStrategyResponse.setStatus(PaymentStatus.REFUNDED); // 策略返回的状态

        // 3. "编排" Mock 对象的行为
        // 当根据 ID 查找支付记录时，返回我们准备好的对象
        when(paymentRepository.findById(123L)).thenReturn(Optional.of(completedPayment));
        // 当调用退款策略时，返回成功响应
        when(strategyProcessor.refundPayment("txn_123456789", "STRIPE", 5000, "CNY"))
                .thenReturn(refundStrategyResponse);
        // 模拟 save 操作会成功
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));


        // --- When (执行) ---

        // 调用退款方法，amountToRefund 为 null 表示全额退款
        PaymentResponseDto finalResponse = paymentService.refundPayment("123", null);


        // --- Then (断言/验证) ---

        // 1. 验证最终返回的响应
        assertThat(finalResponse.getStatus()).isEqualTo(PaymentStatus.REFUNDED);

        // 2. 验证核心的交互行为
        // 验证确实调用了退款策略
        verify(strategyProcessor).refundPayment("txn_123456789", "STRIPE", 5000, "CNY");
        // 验证确实从司机钱包扣款了
        verify(walletService).subtractFunds(88L, 5000);
        // 验证确实发送了退款成功事件
        verify(paymentEventProducer).sendPaymentEvent(any(PaymentEventDto.class));

        // 3. 验证最终保存到数据库的 Payment 对象的状态是否正确
        verify(paymentRepository).save(argThat(savedPayment ->
                savedPayment.getStatus() == PaymentStatus.REFUNDED && // 状态应变为 REFUNDED
                        savedPayment.getRefundedAmount() == 5000 // 已退款金额应等于订单金额
        ));
    }


    @Test
    @DisplayName("当退款的支付ID不存在时，应抛出 ResourceNotFoundException")
    void refundPayment_shouldThrowException_whenPaymentNotFound() {
        // --- Given (安排/准备) ---
        // 关键：当根据ID查找支付记录时，我们返回一个空的 Optional，模拟“未找到”
        when(paymentRepository.findById(anyLong())).thenReturn(Optional.empty());

        // --- When & Then (执行与验证) ---
        // 我们断言，当调用 refundPayment 时...
        assertThatThrownBy(() -> {
            paymentService.refundPayment("404", null); // 使用一个不存在的ID "404"
        })
                .isInstanceOf(ResourceNotFoundException.class) // ...会抛出 ResourceNotFoundException 异常
                .hasMessage("支付记录 404 未找到"); // ...并且异常信息符合预期

        // 验证在找不到支付记录后，后续所有服务都不会被调用
        verifyNoInteractions(strategyProcessor, walletService, paymentEventProducer);
    }
    // 将此方法添加到 PaymentServiceImplTest 类中

    @Test
    @DisplayName("当支付状态不为 COMPLETED 或 PARTIALLY_REFUNDED 时，应抛出异常")
    void refundPayment_shouldThrowException_forInvalidPaymentStatus() {
        // --- Given ---
        // 准备一个状态为 FAILED 的支付记录
        Payment failedPayment = new Payment();
        failedPayment.setId(124L);
        failedPayment.setStatus(PaymentStatus.FAILED); // 状态是 FAILED

        when(paymentRepository.findById(124L)).thenReturn(Optional.of(failedPayment));

        // --- When & Then ---
        assertThatThrownBy(() -> {
            paymentService.refundPayment("124", 500);
        })
                .isInstanceOf(PaymentServiceException.class) // 验证异常类型
                .hasMessage("支付状态为 FAILED, 无法退款。"); // 验证异常信息
    }

    // 将此方法添加到 PaymentServiceImplTest 类中

    @Test
    @DisplayName("当请求退款的金额超过可退款余额时，应抛出异常")
    void refundPayment_shouldThrowException_whenRefundAmountExceedsBalance() {
        // --- Given ---
        Payment completedPayment = new Payment();
        completedPayment.setId(125L);
        completedPayment.setAmount(5000); // 总金额 5000
        completedPayment.setRefundedAmount(0); // 已退款 0
        completedPayment.setStatus(PaymentStatus.COMPLETED);

        when(paymentRepository.findById(125L)).thenReturn(Optional.of(completedPayment));

        // --- When & Then ---
        // 请求退款 6000，这超过了总金额 5000
        assertThatThrownBy(() -> {
            paymentService.refundPayment("125", 6000);
        })
                .isInstanceOf(PaymentServiceException.class)
                .hasMessage("退款金额超过可退款余额。");
    }

    // 将此方法添加到 PaymentServiceImplTest 类中

    @Test
    @DisplayName("当进行部分退款时，应更新已退款金额并将状态设为 PARTIALLY_REFUNDED")
    void refundPayment_shouldSucceedForPartialRefund() {
        // --- Given ---
        Payment completedPayment = new Payment();
        completedPayment.setId(126L);
        completedPayment.setDriverId(88L);
        completedPayment.setAmount(5000);
        completedPayment.setRefundedAmount(0); // 初始已退款 0
        completedPayment.setStatus(PaymentStatus.COMPLETED);
        completedPayment.setTransactionId("txn_partial");
        completedPayment.setPaymentGateway("STRIPE");
        completedPayment.setCurrency("CNY");

        PaymentResponseDto refundStrategyResponse = new PaymentResponseDto();
        // 注意：即使是部分退款，很多网关策略的响应状态也是一个通用的成功状态，
        // 我们的业务代码会根据金额来决定最终状态是 PARTIALLY_REFUNDED 还是 REFUNDED。
        refundStrategyResponse.setStatus(PaymentStatus.REFUNDED);

        when(paymentRepository.findById(126L)).thenReturn(Optional.of(completedPayment));
        // 模拟退款策略对 2000 元的退款请求成功
        when(strategyProcessor.refundPayment("txn_partial", "STRIPE", 2000, "CNY"))
                .thenReturn(refundStrategyResponse);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        // --- When ---
        // 明确请求退款 2000
        paymentService.refundPayment("126", 2000);

        // --- Then ---
        // 验证从司机钱包扣除了正确的金额
        verify(walletService).subtractFunds(88L, 2000);

        // 验证保存到数据库的 Payment 对象状态是否正确
        verify(paymentRepository).save(argThat(savedPayment ->
                // 关键验证：状态应变为 PARTIALLY_REFUNDED
                savedPayment.getStatus() == PaymentStatus.PARTIALLY_REFUNDED &&
                        // 关键验证：已退款金额应为 2000
                        savedPayment.getRefundedAmount() == 2000
        ));
    }

    // 将此方法添加到 PaymentServiceImplTest 类中

    @Test
    @DisplayName("当收到一个成功的支付通知时，应更新支付状态为 COMPLETED 并给钱包加款")
    void handlePaymentNotification_shouldUpdateStatusToCompleted_onSuccessNotification() throws Exception {
        // --- Given (安排/准备) ---

        // 1. 准备一个处于 PENDING 状态的支付记录
        Payment pendingPayment = new Payment();
        pendingPayment.setId(127L);
        pendingPayment.setOrderId(200L);
        pendingPayment.setDriverId(88L);
        pendingPayment.setAmount(7500);
        pendingPayment.setStatus(PaymentStatus.PENDING); // 初始状态为 PENDING

        // 2. 准备一个模拟的 JSON 通知负载
        String successNotification = "{\"OrderId\":\"200\", \"status\":\"SUCCESS\"}";

        // 3. "编排" Mock 对象的行为
        // 当根据 OrderId 查找时，返回我们准备的 pendingPayment
        when(paymentRepository.findByOrderId(200L)).thenReturn(Optional.of(pendingPayment));
        // 模拟 save 会成功
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        // --- When (执行) ---
        paymentService.handlePaymentNotification(successNotification);

        // --- Then (断言/验证) ---

        // 1. 验证钱包加款方法被正确调用
        verify(walletService).addFunds(88L, 7500);

        // 2. 验证支付记录被保存，并且其状态被更新为 COMPLETED
        verify(paymentRepository).save(argThat(savedPayment ->
                savedPayment.getStatus() == PaymentStatus.COMPLETED
        ));
    }
    // 将此方法添加到 PaymentServiceImplTest 类中

    @Test
    @DisplayName("当收到一个失败的支付通知时，应更新支付状态为 FAILED 且不操作钱包")
    void handlePaymentNotification_shouldUpdateStatusToFailed_onFailureNotification() throws Exception {
        // --- Given ---
        Payment pendingPayment = new Payment();
        pendingPayment.setId(128L);
        pendingPayment.setOrderId(201L);
        pendingPayment.setStatus(PaymentStatus.PENDING);

        // 模拟一个失败的通知
        String failureNotification = "{\"OrderId\":\"201\", \"status\":\"FAILURE\"}";

        when(paymentRepository.findByOrderId(201L)).thenReturn(Optional.of(pendingPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        // --- When ---
        paymentService.handlePaymentNotification(failureNotification);

        // --- Then ---

        // 关键验证：钱包服务完全不应该被调用
        verifyNoInteractions(walletService);

        // 验证保存的支付记录状态为 FAILED
        verify(paymentRepository).save(argThat(savedPayment ->
                savedPayment.getStatus() == PaymentStatus.FAILED
        ));
    }

    // 将此方法添加到 PaymentServiceImplTest 类中

    @Test
    @DisplayName("当收到一个已处理过的支付通知时，应直接忽略不进行任何操作")
    void handlePaymentNotification_shouldIgnore_forAlreadyProcessedPayment() throws Exception {
        // --- Given ---
        // 准备一个已经是 COMPLETED 状态的支付记录
        Payment completedPayment = new Payment();
        completedPayment.setId(129L);
        completedPayment.setOrderId(202L);
        completedPayment.setStatus(PaymentStatus.COMPLETED); // 状态已完成

        String duplicateNotification = "{\"OrderId\":\"202\", \"status\":\"SUCCESS\"}";

        when(paymentRepository.findByOrderId(202L)).thenReturn(Optional.of(completedPayment));

        // --- When ---
        paymentService.handlePaymentNotification(duplicateNotification);

        // --- Then ---

        // 关键验证：因为支付状态不是 PENDING，程序应该在检查后直接返回，
        // 所以 save, walletService 等后续操作都不应该被调用。
        verify(paymentRepository, never()).save(any(Payment.class));
        verifyNoInteractions(walletService, paymentEventProducer);
    }

    // 将此方法添加到 PaymentServiceImplTest 类中

    @Test
    @DisplayName("当收到的通知内容格式错误时，应记录错误且不抛出未捕获异常")
    void handlePaymentNotification_shouldLogAndGracefullyExit_forMalformedPayload() {
        // --- Given ---
        String malformedNotification = "this is not a valid json";

        // --- When & Then ---
        // 我们只是调用方法，并期望它不要抛出异常。
        // 在实际代码中，异常被捕获并记录了日志。
        // 在单元测试中，我们通常不直接验证日志输出（这会比较麻烦），
        // 而是验证它没有因为异常而崩溃，并且没有执行任何后续的业务逻辑。
        paymentService.handlePaymentNotification(malformedNotification);

        // 验证没有任何数据库或服务的交互发生
        verifyNoInteractions(paymentRepository, walletService, paymentEventProducer);
    }


}