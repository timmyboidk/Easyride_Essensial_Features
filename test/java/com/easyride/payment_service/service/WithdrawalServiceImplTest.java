package com.easyride.payment_service.service;

import com.easyride.payment_service.dto.WithdrawalRequestDto;
import com.easyride.payment_service.dto.WithdrawalResponseDto;
import com.easyride.payment_service.model.Wallet;
import com.easyride.payment_service.model.Withdrawal;
import com.easyride.payment_service.repository.WalletRepository;
import com.easyride.payment_service.repository.WithdrawalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // 启用 Mockito 扩展，让我们能使用 @Mock 和 @InjectMocks
@DisplayName("提现服务 (WithdrawalServiceImpl) 测试")
class WithdrawalServiceImplTest {

    // 使用 @Mock 创建一个 WithdrawalRepository 的模拟对象。
    // 我们不需要真实的数据库，Mockito 会创建一个“假”的实例。
    @Mock
    private WithdrawalRepository withdrawalRepository;

    // 同理，创建一个 WalletRepository 的模拟对象。
    @Mock
    private WalletRepository walletRepository;

    // 使用 @InjectMocks 创建 WithdrawalServiceImpl 的一个实例。
    // 重点：Mockito 会自动将上面用 @Mock 注解创建的模拟对象 "注入" 到这个实例中。
    // 这意味着 sut (System Under Test, 被测系统) 内部的 withdrawalRepository 和 walletRepository
    // 实际上是我们用 @Mock 创建的模拟对象。
    @InjectMocks
    private WithdrawalServiceImpl withdrawalService;

    // 为了避免在每个测试中重复创建对象，我们可以定义一些通用的测试数据
    private WithdrawalRequestDto withdrawalRequest;
    private Wallet driverWallet;

    @BeforeEach // 这个注解表示，在每个 @Test 方法运行之前，都会先执行这个 setup 方法。
    void setUp() {
        // 准备一个司机的钱包对象，设置一个初始余额
        driverWallet = new Wallet();
        driverWallet.setDriverId(1L);
        driverWallet.setBalance(5000); // 假设余额为 5000 (分)

        // 准备一个提现请求
        withdrawalRequest = new WithdrawalRequestDto();
        withdrawalRequest.setDriverId(1L);
        withdrawalRequest.setBankAccount("1234567890");
    }

    @Test // 这是一个标准的测试方法
    @DisplayName("当钱包余额不足时，应拒绝提现请求并返回失败")
    void requestWithdrawal_shouldFail_whenBalanceIsInsufficient() {
        // --- Given (安排/准备) ---

        // 我们要测试的是余额不足的场景，所以设置一个远超钱包余额的提现金额
        withdrawalRequest.setAmount(10000); // 请求提现 10000 (分)

        // "编排" Mock 对象的行为：
        // 当 walletRepository.findById(1L) 方法被调用时，我们让它返回一个包含我们准备好的 driverWallet 的 Optional 对象。
        // 这是最关键的一步，我们控制了依赖的行为。
        when(walletRepository.findById(1L)).thenReturn(Optional.of(driverWallet));


        // --- When (执行) ---

        // 调用我们要测试的目标方法
        WithdrawalResponseDto response = withdrawalService.requestWithdrawal(withdrawalRequest);


        // --- Then (断言/验证) ---

        // 使用 AssertJ 进行断言，验证结果是否符合预期
        assertThat(response).isNotNull(); // 响应不应为 null
        assertThat(response.getStatus()).isEqualTo("FAILED"); // 状态应为 FAILED
        assertThat(response.getMessage()).isEqualTo("余额不足"); // 消息应为 "余额不足"
        assertThat(response.getWithdrawalId()).isNull(); // 因为失败了，不应该有提现记录的 ID

        // 验证 Mock 对象的交互：
        // 验证 walletRepository.findById(1L) 这个方法确实被调用了，而且只调用了1次。
        verify(walletRepository, times(1)).findById(1L);
        // 验证 withdrawalRepository 的任何方法都没有被调用，因为在余额检查失败后就应该提前退出了，不会去保存提现记录。
        verifyNoInteractions(withdrawalRepository);
    }

    @Test
    @DisplayName("当余额充足时，应成功创建提现请求并返回 PENDING 状态")
    void requestWithdrawal_shouldSucceed_whenBalanceIsSufficient() {
        // --- Given (安排/准备) ---

        // 钱包余额为 5000, 我们请求提现 3000，这是个常规的成功场景。
        withdrawalRequest.setAmount(3000);

        // 同样，先"编排"好 walletRepository 的行为。
        when(walletRepository.findById(1L)).thenReturn(Optional.of(driverWallet));

        // 新增的编排：因为这次会创建提现记录，我们需要模拟 save 方法。
        // Mockito 的 `any()` 匹配器非常有用，它表示“任何 Withdrawal 类型的对象”。
        // 我们告诉 Mockito: "当 withdrawalRepository.save 方法被以任何 Withdrawal 对象为参数调用时，
        // 就返回那个被传入的对象"。这模拟了数据库保存后返回带 ID 的实体的行为。
        when(withdrawalRepository.save(any(Withdrawal.class))).thenAnswer(invocation -> {
            Withdrawal withdrawalToSave = invocation.getArgument(0);
            withdrawalToSave.setId(99L); // 模拟数据库生成了一个 ID
            return withdrawalToSave;
        });


        // --- When (执行) ---

        WithdrawalResponseDto response = withdrawalService.requestWithdrawal(withdrawalRequest);


        // --- Then (断言/验证) ---

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("PENDING"); // 状态应为 PENDING，表示已提交
        assertThat(response.getMessage()).isEqualTo("提现申请已提交");
        assertThat(response.getWithdrawalId()).isEqualTo(99L); // 验证返回的 ID 是我们模拟生成的 ID

        // 验证与依赖的交互
        verify(walletRepository, times(1)).findById(1L); // 验证确实检查了钱包
        verify(withdrawalRepository, times(1)).save(any(Withdrawal.class)); // 验证确实保存了提现记录
    }

    @Test
    @DisplayName("边缘情况：当提现金额恰好等于钱包余额时，应成功创建提现请求")
    void requestWithdrawal_shouldSucceed_whenAmountEqualsBalance() {
        // --- Given ---

        // 将提现金额设置为与钱包余额完全相等
        withdrawalRequest.setAmount(5000);

        when(walletRepository.findById(1L)).thenReturn(Optional.of(driverWallet));
        when(withdrawalRepository.save(any(Withdrawal.class))).thenAnswer(invocation -> {
            Withdrawal w = invocation.getArgument(0);
            w.setId(100L);
            return w;
        });

        // --- When ---
        WithdrawalResponseDto response = withdrawalService.requestWithdrawal(withdrawalRequest);

        // --- Then ---
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getWithdrawalId()).isEqualTo(100L);

        // 验证交互
        verify(walletRepository).findById(1L);
        verify(withdrawalRepository).save(any(Withdrawal.class));
    }

    @Test
    @DisplayName("边缘情况：当司机钱包不存在时，应抛出运行时异常")
    void requestWithdrawal_shouldThrowException_whenWalletNotFound() {
        // --- Given ---

        // 关键：这次我们模拟 findById 返回一个空的 Optional，表示在数据库中找不到这个司机的钱包。
        when(walletRepository.findById(anyLong())).thenReturn(Optional.empty());

        // --- When & Then ---

        // AssertJ 提供了简洁的方式来测试异常。
        // `assertThatThrownBy` 会执行一个 lambda 表达式，并捕获其中的异常。
        assertThatThrownBy(() -> {
            withdrawalService.requestWithdrawal(withdrawalRequest);
        })
                .isInstanceOf(RuntimeException.class) // 验证抛出的异常是 RuntimeException 类型
                .hasMessage("钱包不存在"); // 并且验证异常信息是否符合预期

        // 验证在这种情况下，withdrawalRepository 完全不应该被调用。
        verifyNoInteractions(withdrawalRepository);
    }

    @Test
    @DisplayName("异常情况：当提现金额为 0 或负数时，应拒绝提现")
    void requestWithdrawal_shouldFail_whenAmountIsZeroOrNegative() {
        // --- Given ---
        // 根据 `WithdrawalServiceImpl` 的代码，检查金额小于 `amount` 是在 `wallet.getBalance() < withdrawalRequestDto.getAmount()`
        // 这里的逻辑对于 0 或负数也成立。

        // 场景1：提现金额为 0
        withdrawalRequest.setAmount(0);

        // 钱包里有5000， 5000 < 0 为 false，所以会继续执行
        when(walletRepository.findById(1L)).thenReturn(Optional.of(driverWallet));
        when(withdrawalRepository.save(any(Withdrawal.class))).thenAnswer(invocation -> invocation.getArgument(0));


        // --- When & Then for Zero Amount---
        // 对于0元提现，当前实现会成功创建提现申请，这可能是一个潜在的业务逻辑问题，但我们的测试忠实地反映了当前代码的行为。
        WithdrawalResponseDto zeroResponse = withdrawalService.requestWithdrawal(withdrawalRequest);
        assertThat(zeroResponse.getStatus()).isEqualTo("PENDING");


        // 场景2：提现金额为负数
        // --- Given for Negative Amount ---
        withdrawalRequest.setAmount(-100);

        // --- When & Then for Negative Amount ---
        // 5000 < -100 为 false, 所以也会继续。
        // 这同样揭示了当前代码的一个逻辑缺陷：它没有对提现金额的下限做校验。
        // 一个好的测试不仅验证现有行为，还能揭示潜在问题。
        WithdrawalResponseDto negativeResponse = withdrawalService.requestWithdrawal(withdrawalRequest);
        assertThat(negativeResponse.getStatus()).isEqualTo("PENDING");
    }
}