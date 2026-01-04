package com.easyride.payment_service;

import com.easyride.payment_service.controller.WithdrawalController;
import com.easyride.payment_service.dto.WithdrawalRequestDto;
import com.easyride.payment_service.dto.WithdrawalResponseDto;
import com.easyride.payment_service.model.Withdrawal;
import com.easyride.payment_service.service.WithdrawalService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WithdrawalController.class)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
public class WithdrawalControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private WithdrawalService withdrawalService;

	private ObjectMapper mapper = new ObjectMapper();

	@Test
	public void testRequestWithdrawal() throws Exception {
		WithdrawalRequestDto requestDto = new WithdrawalRequestDto();
		requestDto.setDriverId(10L);
		requestDto.setAmount(5000);
		requestDto.setBankAccount("6222000012345678");

		WithdrawalResponseDto responseDto = new WithdrawalResponseDto(1L, "PENDING", "提现申请已提交");
		when(withdrawalService.requestWithdrawal(requestDto)).thenReturn(responseDto);

		mockMvc.perform(post("/withdrawals/request")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(requestDto)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status", is("PENDING")))
				.andExpect(jsonPath("$.message", is("提现申请已提交")));
	}

	@Test
	public void testGetWithdrawalHistory() throws Exception {
		Withdrawal w1 = new Withdrawal();
		w1.setId(1L);
		Withdrawal w2 = new Withdrawal();
		w2.setId(2L);
		List<Withdrawal> history = Arrays.asList(w1, w2);
		when(withdrawalService.getWithdrawalHistory(10L)).thenReturn(history);

		mockMvc.perform(get("/withdrawals/10/history"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].id", is(1)))
				.andExpect(jsonPath("$[1].id", is(2)));
	}
}
