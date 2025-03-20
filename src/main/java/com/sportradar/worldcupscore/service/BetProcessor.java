package com.sportradar.worldcupscore.service;


import com.sportradar.worldcupscore.model.Bet;
import com.sportradar.worldcupscore.model.BetStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.DoubleAdder;

@Service
public class BetProcessor {

    private static final Logger logger = LoggerFactory.getLogger(BetProcessor.class);

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

    public BetProcessor() {
        executor = Executors.newFixedThreadPool(4);
        for (int i = 0; i < 4; i++) {
            executor.submit(this::processBets);
        }
    }

    public void addBet(Bet bet) {
        if (!isShutdown) {
            betQueue.offer(bet);
        } else {
            logger.info("The system is shutting down. New bets are not being accepted.");
        }
    }

    private void processBets() {
        try {
            while (!isShutdown || !betQueue.isEmpty()) {
                Bet bet = betQueue.poll(100, TimeUnit.MILLISECONDS);
                if (bet != null) {
                    // simulation process
                    Thread.sleep(50);
                    processBet(bet);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void processBet(Bet bet) {
        BetStatus previousStatus = betStatusMap.get(bet.getId());
        boolean valid = false;

        if (!isValidBet(bet, previousStatus, valid)) {
            reviewBets.add(bet);
            logger.info("Bet {} is flagged for review due to invalid sequence: {}", bet.getId(), bet.getStatus());
            return;
        }

        betStatusMap.put(bet.getId(), bet.getStatus());

        totalProcessed.incrementAndGet();
        totalAmount.add(bet.getAmount());

        double result;
        if (bet.getStatus() == BetStatus.WINNER) {
            result = bet.getAmount() * (bet.getOdds() - 1);
            totalProfitLoss.add(result);
            profitPerClient.computeIfAbsent(bet.getClient(), k -> new DoubleAdder()).add(result);
        } else if (bet.getStatus() == BetStatus.LOSER) {
            result = -bet.getAmount();
            totalProfitLoss.add(result);
            lossPerClient.computeIfAbsent(bet.getClient(), k -> new DoubleAdder()).add(bet.getAmount());
        }
    }

    private static boolean isValidBet(Bet bet, BetStatus previousStatus, boolean valid) {
        if (previousStatus == null) {
            // first update: must OPEN
            if (bet.getStatus() == BetStatus.OPEN) {
                valid = true;
            }
        } else if (previousStatus == BetStatus.OPEN) {
            // only final status
            if (bet.getStatus() == BetStatus.WINNER || bet.getStatus() == BetStatus.LOSER || bet.getStatus() == BetStatus.VOID) {
                valid = true;
            }
        }
        return valid;
    }

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
        logger.info("System shutdown completed.");
        logger.info(getSummary());
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
