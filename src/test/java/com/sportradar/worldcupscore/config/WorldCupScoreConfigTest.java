package com.sportradar.worldcupscore.config;

import com.sportradar.worldcupscore.model.Bet;
import com.sportradar.worldcupscore.model.BetStatus;
import com.sportradar.worldcupscore.service.BetProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WorldCupScoreConfigTest {

    private BetProcessor betProcessor;
    private WorldCupScoreConfig config;

    @BeforeEach
    void setUp() {
        // Creamos un mock del BetProcessor
        betProcessor = mock(BetProcessor.class);
        config = new WorldCupScoreConfig(betProcessor);
    }

    @Test
    void testInitData() {
        config.initData();
        // Verificamos que se haya llamado a addBet 100 veces
        verify(betProcessor, times(100)).addBet(any(Bet.class));

        // Capturamos las apuestas que se han añadido para comprobar sus propiedades
        ArgumentCaptor<Bet> betCaptor = ArgumentCaptor.forClass(Bet.class);
        verify(betProcessor, times(100)).addBet(betCaptor.capture());
        List<Bet> bets = betCaptor.getAllValues();

        // Se comprueba que cada apuesta tenga los valores esperados
        for (int i = 0; i < 100; i++) {
            Bet bet = bets.get(i);
            int expectedId = i + 1;
            assertEquals(expectedId, bet.getId());
            assertEquals(100.0, bet.getAmount());
            assertEquals(1.5, bet.getOdds());
            assertEquals("Cliente" + expectedId, bet.getClient());
            assertEquals("Evento", bet.getEvent());
            assertEquals("Market1", bet.getMarket());
            assertEquals("Selection1", bet.getSelection());
            assertEquals(BetStatus.OPEN, bet.getStatus());
        }
    }

    @Test
    void testOnShutdown() {
        // Ejecutamos el método onShutdown
        config.onShutdown();

        // Verificamos que se llame al método shutdownSystem del BetProcessor
        verify(betProcessor, times(1)).shutdownSystem();
    }
}
