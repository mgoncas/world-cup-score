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
     * Método auxiliar para inyectar el valor de numberOfWorkers,
     * ya que en un test unitario fuera del contexto Spring la anotación @Value no se procesa.
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

        // Se crea una apuesta válida (primer update debe ser OPEN)
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
        // Se espera un tiempo para que el bet sea procesado
        Thread.sleep(300);

        String summary = processor.getSummary();
        assertTrue(summary.contains("Total bets processed: 1"));
        assertTrue(summary.contains("Total bets amount: 100.0"));
        // Una apuesta OPEN no afecta al profit/loss
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

        // Se crea una apuesta inválida: el primer update debe ser OPEN, pero se envía WINNER
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

        // Primer update: apuesta válida con estado OPEN
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

        // Segundo update: apuesta con estado WINNER (se espera que sea válida, ya que el estado previo era OPEN)
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
        // Se habrán procesado 2 apuestas (el update OPEN y luego el WINNER)
        assertTrue(summary.contains("Total bets processed: 2"));
        // Total amount: 200.0 (100 + 100)
        assertTrue(summary.contains("Total bets amount: 200.0"));
        // Para el WINNER: profit = 100*(1.5-1)=50; la apuesta OPEN no afecta al profit/loss
        assertTrue(summary.contains("Total result (profit/loss): 50.0"));

        processor.shutdownSystem();
    }

    @Test
    void testLoserBetProcessing() throws Exception {
        BetProcessor processor = new BetProcessor();
        setNumberOfWorkers(processor, 1);
        processor.initialize();

        // Primer update: apuesta válida con estado OPEN
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

        // Segundo update: apuesta con estado LOSER (válida porque el estado previo era OPEN)
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
        // Se habrán procesado 2 apuestas, total amount: 300.0
        assertTrue(summary.contains("Total bets processed: 2"));
        assertTrue(summary.contains("Total bets amount: 300.0"));
        // Para el LOSER: resultado = -150.0 (la apuesta OPEN no genera pérdida)
        assertTrue(summary.contains("Total result (profit/loss): -150.0"));

        processor.shutdownSystem();
    }

    @Test
    void testAddBetAfterShutdown() throws Exception {
        BetProcessor processor = new BetProcessor();
        setNumberOfWorkers(processor, 1);
        processor.initialize();

        // Se cierra el sistema
        processor.shutdownSystem();

        // Intentar agregar una apuesta tras el shutdown: la apuesta no debe ser aceptada.
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
        // Se espera un poco para dar chance a que, en caso de haberse aceptado, se procesara
        Thread.sleep(200);

        // Al obtener el resumen, se debe reflejar que no se procesó la apuesta
        String summary = processor.getSummary();
        assertTrue(summary.contains("Total bets processed: 0"));
    }

    @Test
    void testProcessBetsInterruptedExceptionHandling() throws Exception {
        // Limpia el flag de interrupción del hilo actual
        Thread.interrupted();

        BetProcessor processor = new BetProcessor();
        setNumberOfWorkers(processor, 1);
        processor.initialize();

        // Reemplazamos el betQueue por una implementación que siempre arroja InterruptedException
        InterruptingQueue queue = new InterruptingQueue();
        Field betQueueField = BetProcessor.class.getDeclaredField("betQueue");
        betQueueField.setAccessible(true);
        betQueueField.set(processor, queue);

        // Usamos reflexión para invocar el método privado processBets en el hilo actual.
        Method processBetsMethod = BetProcessor.class.getDeclaredMethod("processBets");
        processBetsMethod.setAccessible(true);
        processBetsMethod.invoke(processor);

        // Tras capturar el InterruptedException, el catch invoca Thread.currentThread().interrupt().
        // Verificamos que el flag de interrupción del hilo actual esté activo.
        assertTrue(Thread.currentThread().isInterrupted(), "Thread should be interrupted after catching InterruptedException");
    }

    @Test
    void testShutdownSystemAwaitTerminationFalse() throws Exception {
        // Preparamos la instancia del processor y configuramos el número de workers
        BetProcessor processor = new BetProcessor();
        setNumberOfWorkers(processor, 1);
        processor.initialize();

        // Creamos un fake executor que retorna false en awaitTermination
        FakeExecutorService fakeExecutor = new FakeExecutorService(false, false);
        // Inyectamos el fake executor en el processor
        Field executorField = BetProcessor.class.getDeclaredField("executor");
        executorField.setAccessible(true);
        executorField.set(processor, fakeExecutor);

        // Invocamos shutdownSystem()
        processor.shutdownSystem();

        // Se esperaba que, al no haber terminado la terminación, se invoque shutdownNow()
        assertTrue(fakeExecutor.shutdownNowCalled,
                "shutdownNow should be called when awaitTermination returns false");
    }

    @Test
    void testShutdownSystemInterruptedException() throws Exception {
        // Preparamos la instancia del processor y configuramos el número de workers
        BetProcessor processor = new BetProcessor();
        setNumberOfWorkers(processor, 1);
        processor.initialize();

        // Creamos un fake executor que lanza InterruptedException en awaitTermination
        FakeExecutorService fakeExecutor = new FakeExecutorService(false, true);
        // Inyectamos el fake executor en el processor
        Field executorField = BetProcessor.class.getDeclaredField("executor");
        executorField.setAccessible(true);
        executorField.set(processor, fakeExecutor);

        // Invocamos shutdownSystem(); se espera que capture la InterruptedException
        processor.shutdownSystem();

        // En ambos casos (awaitTermination false o exception) se debe invocar shutdownNow()
        assertTrue(fakeExecutor.shutdownNowCalled,
                "shutdownNow should be called when awaitTermination throws InterruptedException");
    }

    // Clase interna para forzar el lanzamiento de InterruptedException en poll(...)
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
         * @param awaitTerminationShouldReturn valor que retornará awaitTermination si no se lanza excepción.
         * @param throwInterruptedExceptionOnAwait si es true, awaitTermination lanzará InterruptedException.
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

        // Los métodos restantes no son necesarios para estos tests y pueden lanzar UnsupportedOperationException

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
