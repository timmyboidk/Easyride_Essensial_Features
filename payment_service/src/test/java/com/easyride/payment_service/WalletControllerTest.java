package com.easyride.payment_service;

import com.easyride.payment_service.controller.WalletController;
import com.easyride.payment_service.dto.WalletDto;
import com.easyride.payment_service.model.Payment;
import com.easyride.payment_service.service.WalletService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WalletController.class)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
public class WalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WalletService walletService;

    private ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testGetWallet() throws Exception {
        WalletDto walletDto = new WalletDto(10L, 50000, LocalDateTime.now());
        when(walletService.getWallet(10L)).thenReturn(walletDto);

        mockMvc.perform(get("/wallets/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.driverId", is(10)))
                .andExpect(jsonPath("$.balance", is(50000)));
    }

    @Test
    public void testGetEarnings() throws Exception {
        Payment p1 = new Payment();
        p1.setId(1L);
        p1.setOrderId(100L);
        Payment p2 = new Payment();
        p2.setId(2L);
        p2.setOrderId(101L);
        List<Payment> earnings = Arrays.asList(p1, p2);
        when(walletService.getEarnings(10L, LocalDateTime.parse("2025-04-01T00:00:00"),
                LocalDateTime.parse("2025-04-30T23:59:59")))
                .thenReturn(earnings);

        mockMvc.perform(get("/wallets/10/earnings")
                .param("from", "2025-04-01T00:00:00")
                .param("to", "2025-04-30T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[1].id", is(2)));
    }
}
