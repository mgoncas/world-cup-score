package com.sportradar.worldcupscore.controller;

import com.sportradar.worldcupscore.model.Bet;
import com.sportradar.worldcupscore.model.BetStatus;
import com.sportradar.worldcupscore.service.BetProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class BetControllerTest {

    private BetProcessor betProcessor;
    private BetController betController;

    @BeforeEach
    void setUp() {
        betProcessor = mock(BetProcessor.class);
        betController = new BetController(betProcessor);
    }

    @Test
    void testAddBet() {
        Bet bet = new Bet.BetBuilder()
                .id(1)
                .amount(100.0)
                .odds(1.5)
                .client("TestClient")
                .event("TestEvent")
                .market("TestMarket")
                .selection("TestSelection")
                .status(BetStatus.OPEN)
                .build();

        ResponseEntity<Bet> response = betController.addBet(bet);

        verify(betProcessor, times(1)).addBet(bet);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(bet, response.getBody());
    }

    @Test
    void testGetSummary() {
        String summary = "Test summary";
        when(betProcessor.getSummary()).thenReturn(summary);

        ResponseEntity<String> response = betController.getSummary();

        verify(betProcessor, times(1)).getSummary();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(summary, response.getBody());
    }

    @Test
    void testGetReviewBets() {
        Bet bet1 = new Bet.BetBuilder()
                .id(1)
                .amount(100.0)
                .odds(1.5)
                .client("TestClient1")
                .event("TestEvent")
                .market("TestMarket")
                .selection("TestSelection")
                .status(BetStatus.OPEN)
                .build();

        Bet bet2 = new Bet.BetBuilder()
                .id(2)
                .amount(200.0)
                .odds(2.5)
                .client("TestClient2")
                .event("TestEvent")
                .market("TestMarket")
                .selection("TestSelection")
                .status(BetStatus.WINNER)
                .build();

        List<Bet> betList = Arrays.asList(bet1, bet2);
        when(betProcessor.getReviewBets()).thenReturn(betList);

        ResponseEntity<List<Bet>> response = betController.getReviewBets();

        verify(betProcessor, times(1)).getReviewBets();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(betList, response.getBody());
    }

    @Test
    void testShutdownSystem() {
        doNothing().when(betProcessor).shutdownSystem();

        ResponseEntity<String> response = betController.shutdownSystem();

        verify(betProcessor, times(1)).shutdownSystem();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("System shutdown initiated.", response.getBody());
    }
}
