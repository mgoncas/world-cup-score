package org.worldcup;

import org.worldcup.model.Bet;
import org.worldcup.model.BetStatus;
import org.worldcup.service.BetProcessor;

public class Main {
    public static void main(String[] args) {
        BetProcessor processor = new BetProcessor(4); // 4 workers

        // Inicialización de 100 apuestas de ejemplo
        for (int i = 1; i <= 100; i++) {
            // Primera actualización de la apuesta, debe ser OPEN
            Bet bet = new Bet.BetBuilder()
                    .id(i)
                    .amount(100.0)
                    .odds(1.5)
                    .client("Cliente" + i)
                    .event("Evento")
                    .market("Market1")
                    .selection("Selection1")
                    .status(BetStatus.OPEN)
                    .build();
            processor.addBet(bet);

            BetStatus finalStatus;
            if (i % 3 == 0) {
                finalStatus = BetStatus.WINNER;
            } else if (i % 3 == 1) {
                finalStatus = BetStatus.LOSER;
            } else {
                finalStatus = BetStatus.VOID;
            }
            new Thread(() -> {
                try {
                    Thread.sleep(100); // Simula retardo antes de actualizar a estado final
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                Bet finalBet = new Bet.BetBuilder()
                        .id(bet.getId())
                        .amount(bet.getAmount())
                        .odds(bet.getOdds())
                        .client(bet.getClient())
                        .event(bet.getEvent())
                        .market(bet.getMarket())
                        .selection(bet.getSelection())
                        .status(finalStatus)
                        .build();
                processor.addBet(finalBet);
            }).start();
        }

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        processor.shutdownSystem();
    }
}