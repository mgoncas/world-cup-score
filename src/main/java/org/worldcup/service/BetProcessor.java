package org.worldcup.service;

import org.worldcup.model.Bet;
import org.worldcup.model.BetStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.DoubleAdder;

public class BetProcessor {

    private final BlockingQueue<Bet> betQueue = new LinkedBlockingQueue<>();

    private final ExecutorService executor;
    private volatile boolean isShutdown = false;

    private final ConcurrentHashMap<Integer, BetStatus> betStatusMap = new ConcurrentHashMap<>();

    private final AtomicInteger totalProcessed = new AtomicInteger(0);
    private final DoubleAdder totalAmount = new DoubleAdder();
    private final DoubleAdder totalProfitLoss = new DoubleAdder();


    private final ConcurrentHashMap<String, DoubleAdder> profitPerClient = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, DoubleAdder> lossPerClient = new ConcurrentHashMap<>();

    private final List<Bet> reviewBets = Collections.synchronizedList(new ArrayList<>());

    // Constructor que inicia el ExecutorService con el número de workers especificado
    public BetProcessor(int numWorkers) {
        executor = Executors.newFixedThreadPool(numWorkers);
        for (int i = 0; i < numWorkers; i++) {
            executor.submit(this::processBets);
        }
    }

    // Método para agregar una apuesta (simula un endpoint REST POST /bets)
    public void addBet(Bet bet) {
        if (!isShutdown) {
            betQueue.offer(bet);
        } else {
            System.out.println("The system is shutting down. New bets are not being accepted.");
        }
    }

    // Worker que procesa apuestas en un bucle
    private void processBets() {
        try {
            while (!isShutdown || !betQueue.isEmpty()) {
                Bet bet = betQueue.poll(100, TimeUnit.MILLISECONDS);
                if (bet != null) {
                    // Simulación del tiempo de procesamiento (50ms)
                    Thread.sleep(50);
                    processBet(bet);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Lógica de procesamiento de una apuesta individual
    private void processBet(Bet bet) {
        // Validación de la secuencia de estados
        BetStatus previousStatus = betStatusMap.get(bet.getId());
        boolean valid = false;

        valid = isValidBet(bet, previousStatus, valid);

        if (!valid) {
            // Apuesta inválida, se marca para revisión
            reviewBets.add(bet);
            System.out.println("Bet " + bet.getId() + " is flagged for review due to invalid sequence: " + bet.getStatus());
            return;
        }

        // Actualiza el último estado conocido para la apuesta
        betStatusMap.put(bet.getId(), bet.getStatus());

        // Actualiza estadísticas globales
        totalProcessed.incrementAndGet();
        totalAmount.add(bet.getAmount());

        double result = 0.0;
        if (bet.getStatus() == BetStatus.WINNER) {
            result = bet.getAmount() * (bet.getOdds() - 1);
            totalProfitLoss.add(result);
            profitPerClient.computeIfAbsent(bet.getClient(), k -> new DoubleAdder()).add(result);
        } else if (bet.getStatus() == BetStatus.LOSER) {
            result = -bet.getAmount();
            totalProfitLoss.add(result);
            lossPerClient.computeIfAbsent(bet.getClient(), k -> new DoubleAdder()).add(bet.getAmount());
        } else if (bet.getStatus() == BetStatus.VOID) {
            // VOID: se reembolsa, sin efecto en ganancia/pérdida.
            result = 0.0;
        }
    }

    private static boolean isValidBet(Bet bet, BetStatus previousStatus, boolean valid) {
        if (previousStatus == null) {
            // Primera actualización: debe ser OPEN
            if (bet.getStatus() == BetStatus.OPEN) {
                valid = true;
            }
        } else if (previousStatus == BetStatus.OPEN) {
            // Transición válida: solo se permiten estados finales
            if (bet.getStatus() == BetStatus.WINNER || bet.getStatus() == BetStatus.LOSER || bet.getStatus() == BetStatus.VOID) {
                valid = true;
            }
        }
        return valid;
    }

    // Método para apagar el sistema de forma controlada (simula POST /shutdown)
    public void shutdownSystem() {
        isShutdown = true;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
        System.out.println("System shutdown completed.");
        System.out.println(getSummary());
    }

    // Método para generar un resumen de las estadísticas procesadas
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Total bets processed: ").append(totalProcessed.get()).append("\n");
        sb.append("Total bets amount: ").append(totalAmount.sum()).append("\n");
        sb.append("Total result (profit/loss): ").append(totalProfitLoss.sum()).append("\n");

        sb.append("Top 5 customers with the highest winnings: \n");
        profitPerClient.entrySet().stream()
                .sorted((e1, e2) -> Double.compare(e2.getValue().sum(), e1.getValue().sum()))
                .limit(5)
                .forEach(e -> sb.append(e.getKey()).append(": ").append(e.getValue().sum()).append("\n"));

        sb.append("Top 5 customers with the highest losses: \n");
        lossPerClient.entrySet().stream()
                .sorted((e1, e2) -> Double.compare(e2.getValue().sum(), e1.getValue().sum()))
                .limit(5)
                .forEach(e -> sb.append(e.getKey()).append(": ").append(e.getValue().sum()).append("\n"));

        if (!reviewBets.isEmpty()) {
            sb.append("Bets flagged for review: ").append(reviewBets.size()).append("\n");
        }
        return sb.toString();
    }

}
