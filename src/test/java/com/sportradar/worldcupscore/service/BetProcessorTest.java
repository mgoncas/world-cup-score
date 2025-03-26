package com.sportradar.worldcupscore.service;

import com.sportradar.worldcupscore.model.Bet;
import com.sportradar.worldcupscore.model.BetStatus;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class BetProcessorTest {

    /**
     * Helper method to inject the value of numberOfWorkers,
     * since in a unit test outside of the Spring context, the @Value annotation is not processed.
     */
    private void setNumberOfWorkers(BetProcessor processor, int workers) throws Exception {
        Field field = BetProcessor.class.getDeclaredField("numberOfWorkers");
        field.setAccessible(true);
        field.set(processor, workers);
    }

    @Test
    void testEmptySummary() throws Exception {
        BetProcessor processor = new BetProcessor();
        setNumberOfWorkers(processor, 1);
        processor.initialize();

        String summary = processor.getSummary();
        assertTrue(summary.contains("Total bets processed: 0"));
        assertTrue(summary.contains("Total bets amount: 0.0"));
        assertTrue(summary.contains("Total result (profit/loss): 0.0"));

        processor.shutdownSystem();
    }

    @Test
    void testValidBetProcessing() throws Exception {
        BetProcessor processor = new BetProcessor();
        setNumberOfWorkers(processor, 1);
        processor.initialize();

        // A valid bet is created (first update must be OPEN)
        Bet bet = new Bet.BetBuilder()
                .id(1)
                .amount(100.0)
                .odds(1.5)
                .client("Client1")
                .event("Event1")
                .market("Market1")
                .selection("Selection1")
                .status(BetStatus.OPEN)
                .build();

        processor.addBet(bet);
        // Wait some time to allow the bet to be processed
        Thread.sleep(300);

        String summary = processor.getSummary();
        assertTrue(summary.contains("Total bets processed: 1"));
        assertTrue(summary.contains("Total bets amount: 100.0"));
        // Wait some time to allow the bet to be processed
        assertTrue(summary.contains("Total result (profit/loss): 0.0"));

        List<Bet> review = processor.getReviewBets();
        assertEquals(0, review.size());

        processor.shutdownSystem();
    }

    @Test
    void testInvalidBetProcessing() throws Exception {
        BetProcessor processor = new BetProcessor();
        setNumberOfWorkers(processor, 1);
        processor.initialize();

        // An invalid bet is created: the first update must be OPEN, but WINNER is sent instead
        Bet bet = new Bet.BetBuilder()
                .id(2)
                .amount(200.0)
                .odds(2.0)
                .client("Client2")
                .event("Event2")
                .market("Market2")
                .selection("Selection2")
                .status(BetStatus.WINNER)
                .build();

        processor.addBet(bet);
        Thread.sleep(300);

        List<Bet> review = processor.getReviewBets();
        assertEquals(1, review.size());
        assertEquals(2, review.get(0).getId());

        processor.shutdownSystem();
    }

    @Test
    void testWinnerBetProcessing() throws Exception {
        BetProcessor processor = new BetProcessor();
        setNumberOfWorkers(processor, 1);
        processor.initialize();

        // First update: valid bet with OPEN status
        Bet betOpen = new Bet.BetBuilder()
                .id(3)
                .amount(100.0)
                .odds(1.5)
                .client("Client3")
                .event("Event3")
                .market("Market3")
                .selection("Selection3")
                .status(BetStatus.OPEN)
                .build();
        processor.addBet(betOpen);
        Thread.sleep(300);

        // Second update: bet with WINNER status (expected to be valid since previous state was OPEN)
        Bet betWinner = new Bet.BetBuilder()
                .id(3)
                .amount(100.0)
                .odds(1.5)
                .client("Client3")
                .event("Event3")
                .market("Market3")
                .selection("Selection3")
                .status(BetStatus.WINNER)
                .build();
        processor.addBet(betWinner);
        Thread.sleep(300);

        String summary = processor.getSummary();
        // Two bets should have been processed (OPEN then WINNER)
        assertTrue(summary.contains("Total bets processed: 2"));
        // Total amount: 200.0 (100 + 100)
        assertTrue(summary.contains("Total bets amount: 200.0"));
        // For WINNER: profit = 100*(1.5-1)=50; the OPEN bet doesn't affect profit/loss
        assertTrue(summary.contains("Total result (profit/loss): 50.0"));

        processor.shutdownSystem();
    }

    @Test
    void testLoserBetProcessing() throws Exception {
        BetProcessor processor = new BetProcessor();
        setNumberOfWorkers(processor, 1);
        processor.initialize();

        // First update: valid bet with OPEN status
        Bet betOpen = new Bet.BetBuilder()
                .id(4)
                .amount(150.0)
                .odds(2.0)
                .client("Client4")
                .event("Event4")
                .market("Market4")
                .selection("Selection4")
                .status(BetStatus.OPEN)
                .build();
        processor.addBet(betOpen);
        Thread.sleep(300);

        // Second update: bet with LOSER status (valid since the previous state was OPEN)
        Bet betLoser = new Bet.BetBuilder()
                .id(4)
                .amount(150.0)
                .odds(2.0)
                .client("Client4")
                .event("Event4")
                .market("Market4")
                .selection("Selection4")
                .status(BetStatus.LOSER)
                .build();
        processor.addBet(betLoser);
        Thread.sleep(300);

        String summary = processor.getSummary();
        // Two bets should have been processed; total amount: 300.0
        assertTrue(summary.contains("Total bets processed: 2"));
        assertTrue(summary.contains("Total bets amount: 300.0"));
        // For LOSER: result = -150.0 (OPEN bet does not cause loss)
        assertTrue(summary.contains("Total result (profit/loss): -150.0"));

        processor.shutdownSystem();
    }

    @Test
    void testAddBetAfterShutdown() throws Exception {
        BetProcessor processor = new BetProcessor();
        setNumberOfWorkers(processor, 1);
        processor.initialize();

        // The system is shut down
        processor.shutdownSystem();

        // Try to add a bet after shutdown: it should not be accepted
        Bet bet = new Bet.BetBuilder()
                .id(5)
                .amount(200.0)
                .odds(1.8)
                .client("Client5")
                .event("Event5")
                .market("Market5")
                .selection("Selection5")
                .status(BetStatus.OPEN)
                .build();
        processor.addBet(bet);
        // Wait a bit to give a chance for the bet to be processed in case it was accepted
        Thread.sleep(200);

        // The summary should reflect that the bet was not processed
        String summary = processor.getSummary();
        assertTrue(summary.contains("Total bets processed: 0"));
    }

    @Test
    void testProcessBetsInterruptedExceptionHandling() throws Exception {
        // Clear the current thread's interruption flag
        Thread.interrupted();

        BetProcessor processor = new BetProcessor();
        setNumberOfWorkers(processor, 1);
        processor.initialize();

        // Replace betQueue with an implementation that always throws InterruptedException
        InterruptingQueue queue = new InterruptingQueue();
        Field betQueueField = BetProcessor.class.getDeclaredField("betQueue");
        betQueueField.setAccessible(true);
        betQueueField.set(processor, queue);

        // Use reflection to invoke the private method processBets in the current thread
        Method processBetsMethod = BetProcessor.class.getDeclaredMethod("processBets");
        processBetsMethod.setAccessible(true);
        processBetsMethod.invoke(processor);

        // After catching InterruptedException, the catch block calls Thread.currentThread().interrupt()
        // Verify that the thread interruption flag is now set
        assertTrue(Thread.currentThread().isInterrupted(), "Thread should be interrupted after catching InterruptedException");
    }

    @Test
    void testShutdownSystemAwaitTerminationFalse() throws Exception {
        // Prepare the processor instance and set number of workers
        BetProcessor processor = new BetProcessor();
        setNumberOfWorkers(processor, 1);
        processor.initialize();

        // Create a fake executor that returns false on awaitTermination
        FakeExecutorService fakeExecutor = new FakeExecutorService(false, false);
        // Inject the fake executor into the processor
        Field executorField = BetProcessor.class.getDeclaredField("executor");
        executorField.setAccessible(true);
        executorField.set(processor, fakeExecutor);

        // Call shutdownSystem()
        processor.shutdownSystem();

        // Since awaitTermination returned false, shutdownNow() should have been called
        assertTrue(fakeExecutor.shutdownNowCalled,
                "shutdownNow should be called when awaitTermination returns false");
    }

    @Test
    void testShutdownSystemInterruptedException() throws Exception {
        // Prepare the processor instance and set number of workers
        BetProcessor processor = new BetProcessor();
        setNumberOfWorkers(processor, 1);
        processor.initialize();

        // Create a fake executor that throws InterruptedException during awaitTermination
        FakeExecutorService fakeExecutor = new FakeExecutorService(false, true);
        // Inject the fake executor into the processor
        Field executorField = BetProcessor.class.getDeclaredField("executor");
        executorField.setAccessible(true);
        executorField.set(processor, fakeExecutor);

        // Call shutdownSystem(); it should catch the InterruptedException
        processor.shutdownSystem();

        // Inner class to force InterruptedException during poll(...)
        assertTrue(fakeExecutor.shutdownNowCalled,
                "shutdownNow should be called when awaitTermination throws InterruptedException");
    }


    // Inner class to force InterruptedException during poll(...)
    private static class InterruptingQueue extends LinkedBlockingQueue<Bet> {
        @Override
        public Bet poll(long timeout, TimeUnit unit) throws InterruptedException {
            throw new InterruptedException("Forced interruption");
        }
    }

    private static class FakeExecutorService implements ExecutorService {
        private final boolean awaitTerminationShouldReturn;
        private final boolean throwInterruptedExceptionOnAwait;
        boolean shutdownNowCalled = false;
        boolean shutdownCalled = false;

        /**
         * @param awaitTerminationShouldReturn the value that awaitTermination should return if no exception is thrown
         * @param throwInterruptedExceptionOnAwait if true, awaitTermination will throw InterruptedException
         */
        public FakeExecutorService(boolean awaitTerminationShouldReturn, boolean throwInterruptedExceptionOnAwait) {
            this.awaitTerminationShouldReturn = awaitTerminationShouldReturn;
            this.throwInterruptedExceptionOnAwait = throwInterruptedExceptionOnAwait;
        }

        @Override
        public void shutdown() {
            shutdownCalled = true;
        }

        @Override
        public List<Runnable> shutdownNow() {
            shutdownNowCalled = true;
            return Collections.emptyList();
        }

        @Override
        public boolean isShutdown() {
            return false;
        }

        @Override
        public boolean isTerminated() {
            return false;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            if (throwInterruptedExceptionOnAwait) {
                throw new InterruptedException("Forced interruption");
            }
            return awaitTerminationShouldReturn;
        }

        @Override
        public <T> java.util.concurrent.Future<T> submit(java.util.concurrent.Callable<T> task) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> java.util.concurrent.Future<T> submit(Runnable task, T result) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.concurrent.Future<?> submit(Runnable task) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void execute(Runnable command) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> List<java.util.concurrent.Future<T>> invokeAll(
                java.util.Collection<? extends java.util.concurrent.Callable<T>> tasks) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> List<java.util.concurrent.Future<T>> invokeAll(
                java.util.Collection<? extends java.util.concurrent.Callable<T>> tasks, long timeout, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T invokeAny(java.util.Collection<? extends java.util.concurrent.Callable<T>> tasks) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T invokeAny(java.util.Collection<? extends java.util.concurrent.Callable<T>> tasks, long timeout, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }
    }
}
