package com.sportradar.worldcupscore;

import com.sportradar.worldcupscore.service.BetProcessor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(BetProcessor.class)
class WorldCupScoreApplicationTests {

	@Test
	void contextLoads() {
	}

}
