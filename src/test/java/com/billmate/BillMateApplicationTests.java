package com.billmate;

import com.slack.api.bolt.App;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class BillMateApplicationTests {

	@MockitoBean
	App slackApp;

	@Test
	void contextLoads() {
	}
}
