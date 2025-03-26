package com.sportradar.worldcupscore.config;

import com.sportradar.worldcupscore.model.Bet;
import com.sportradar.worldcupscore.model.BetStatus;
import com.sportradar.worldcupscore.service.BetProcessor;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WorldCupScoreConfig {

    private static final Logger logger = LoggerFactory.getLogger(WorldCupScoreConfig.class);

    private final BetProcessor betProcessor;

    public WorldCupScoreConfig(BetProcessor betProcessor) {
        this.betProcessor = betProcessor;
    }

    @PostConstruct
    public void initData() {
        for (int i = 1; i <= 100; i++) {
            Bet bet = new Bet.BetBuilder()
                    .id(i)
                    .amount(100.0)
                    .odds(1.5)
                    .client("Client" + i)
                    .event("Event")
                    .market("Market1")
                    .selection("Selection1")
                    .status(BetStatus.OPEN)
                    .build();
            betProcessor.addBet(bet);
        }
        logger.info("100 bets with OPEN status have been added when starting the application.");
    }

    @PreDestroy
    public void onShutdown() {
        betProcessor.shutdownSystem();
    }
}
