package dev.crochard.spring_ai_lab;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SpringAiLabApplicationIT {

	@Test
	void contextLoads() {
	}

}
