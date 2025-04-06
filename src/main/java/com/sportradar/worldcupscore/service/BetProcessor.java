package com.sportradar.worldcupscore.service;


import com.sportradar.worldcupscore.model.Bet;
import com.sportradar.worldcupscore.model.BetStatus;
import com.sportradar.worldcupscore.util.Messages;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${bet.processor.workers:4}")
    private int numberOfWorkers;

    private final BlockingQueue<Bet> betQueue = new LinkedBlockingQueue<>();

    private ExecutorService executor;
    private volatile boolean isShutdown = false;

    private final ConcurrentHashMap<Integer, BetStatus> betStatusMap = new ConcurrentHashMap<>();

    private final AtomicInteger totalProcessed = new AtomicInteger(0);
    private final DoubleAdder totalAmount = new DoubleAdder();
    private final DoubleAdder totalProfitLoss = new DoubleAdder();

    private final ConcurrentHashMap<String, DoubleAdder> profitPerClient = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, DoubleAdder> lossPerClient = new ConcurrentHashMap<>();

    private final List<Bet> reviewBets = Collections.synchronizedList(new ArrayList<>());


    @PostConstruct
    public void initialize() {
        executor = Executors.newFixedThreadPool(numberOfWorkers);
        for (int i = 0; i < numberOfWorkers; i++) {
            executor.submit(this::processBets);
        }
    }

    public void addBet(Bet bet) {
        if (!isShutdown) {
            betQueue.offer(bet);
        } else {
            logger.info(Messages.SHUTTING_DOWN);
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

        if (!isValidBet(bet, previousStatus)) {
            reviewBets.add(bet);
            logger.info(Messages.BET_REVIEW, bet.getId(), bet.getStatus());
            return;
        }

        betStatusMap.put(bet.getId(), bet.getStatus());

        totalProcessed.incrementAndGet();

        if (bet.getStatus() == BetStatus.OPEN) {
            // only when open
            totalAmount.add(bet.getAmount());
        }

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

        logger.info(Messages.BET_PROCESSED, bet.getId());
    }

    // FIXME: validar tambien si la apuesta no ha cambiado para dar coherencia??
    private boolean isValidBet(Bet bet, BetStatus previousStatus) {
        if (previousStatus == null) {
            return bet.getStatus() == BetStatus.OPEN;
        }
        if (previousStatus == BetStatus.OPEN) {
            return bet.getStatus() == BetStatus.WINNER ||
                    bet.getStatus() == BetStatus.LOSER ||
                    bet.getStatus() == BetStatus.VOID;
        }
        return false;
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
        logger.info(Messages.SHUTDOWN_COMPLETED);
        logger.info(getSummary());
    }

    // Método para generar un resumen de las estadísticas procesadas
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(Messages.SUMMARY_HEADER_TOTAL_PROCESSED).append(totalProcessed.get()).append("\n");
        sb.append(Messages.SUMMARY_HEADER_TOTAL_AMOUNT).append(totalAmount.sum()).append("\n");
        sb.append(Messages.SUMMARY_HEADER_TOTAL_PROFIT_LOSS).append(totalProfitLoss.sum()).append("\n");

        sb.append(Messages.SUMMARY_HEADER_TOP_WINNERS + "\n");
        profitPerClient.entrySet().stream()
                .sorted((e1, e2) -> Double.compare(e2.getValue().sum(), e1.getValue().sum()))
                .limit(5)
                .forEach(e -> sb.append(e.getKey()).append(": ").append(e.getValue().sum()).append("\n"));

        sb.append(Messages.SUMMARY_HEADER_TOP_LOSERS + "\n");
        lossPerClient.entrySet().stream()
                .sorted((e1, e2) -> Double.compare(e2.getValue().sum(), e1.getValue().sum()))
                .limit(5)
                .forEach(e -> sb.append(e.getKey()).append(": ").append(e.getValue().sum()).append("\n"));

        if (!reviewBets.isEmpty()) {
            sb.append(Messages.SUMMARY_HEADER_REVIEW).append(reviewBets.size()).append("\n");
        }
        return sb.toString();
    }

    public List<Bet> getReviewBets() {
        synchronized (reviewBets) {
            return new ArrayList<>(reviewBets);
        }
    }

}
