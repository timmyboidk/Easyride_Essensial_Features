package com.easyride.matching_service;

import org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@EnableAutoConfiguration(exclude = { RocketMQAutoConfiguration.class })
class MatchingServiceApplicationTests {

	@MockitoBean
	private RocketMQTemplate rocketMQTemplate;

	@Test
	void contextLoads() {
	}

}
