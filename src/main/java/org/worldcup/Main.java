package org.worldcup;

import org.worldcup.model.Bet;
import org.worldcup.model.BetStatus;
import org.worldcup.service.BetProcessor;

import java.util.Random;

public class Main {
    public static void main(String[] args) {
        BetProcessor processor = new BetProcessor(4); // 4 workers
        Random random = new Random();
        BetStatus[] values = BetStatus.values();

        // Inicialización de 50 apuestas con estado OPEN
        for (int i = 1; i <= 50; i++) {
            // Primera actualización de la apuesta, debe ser OPEN
            Bet bet = new Bet.BetBuilder()
                    .id(i)
                    .amount(random.nextDouble(100.0))
                    .odds(random.nextDouble(5.0))
                    .client("Cliente" + i)
                    .event("Evento")
                    .market("Market1")
                    .selection("Selection1")
                    .status(BetStatus.OPEN)
                    .build();
            processor.addBet(bet);
        }

        // Inicialización de 50 apuestas con estado ALEATORIO
        for (int i = 1; i <= 50; i++) {
            // Primera actualización de la apuesta, debe ser OPEN
            Bet bet = new Bet.BetBuilder()
                    .id(i)
                    .amount(random.nextDouble(100.0))
                    .odds(random.nextDouble(5.0))
                    .client("Cliente" + i)
                    .event("Evento")
                    .market("Market1")
                    .selection("Selection1")
                    .status(BetStatus.values()[random.nextInt(4)])
                    .build();
            processor.addBet(bet);
        }

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        processor.shutdownSystem();
    }
}