package com.easyride.admin_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@org.springframework.boot.autoconfigure.EnableAutoConfiguration(exclude = {
		org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration.class })
class AdminServiceApplicationTests {

	@org.springframework.test.context.bean.override.mockito.MockitoBean
	private org.apache.rocketmq.spring.core.RocketMQTemplate rocketMQTemplate;

	@Test
	void contextLoads() {
	}

}
