package com.sportradar.worldcupscore.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BetTest {

    @Test
    void testBetBuilderAndGetters() {
        int id = 10;
        double amount = 150.0;
        double odds = 1.8;
        String client = "TestClient";
        String event = "World Cup Final";
        String market = "Winner";
        String selection = "Team A";
        BetStatus status = BetStatus.OPEN;

        Bet bet = new Bet.BetBuilder()
                .id(id)
                .amount(amount)
                .odds(odds)
                .client(client)
                .event(event)
                .market(market)
                .selection(selection)
                .status(status)
                .build();

        assertEquals(id, bet.getId());
        assertEquals(amount, bet.getAmount());
        assertEquals(odds, bet.getOdds());
        assertEquals(client, bet.getClient());
        assertEquals(event, bet.getEvent());
        assertEquals(market, bet.getMarket());
        assertEquals(selection, bet.getSelection());
        assertEquals(status, bet.getStatus());
    }

    @Test
    void testToStringContainsAllInformation() {
        int id = 5;
        double amount = 100.0;
        double odds = 2.0;
        String client = "ClientTest";
        String event = "EventTest";
        String market = "MarketTest";
        String selection = "SelectionTest";
        BetStatus status = BetStatus.OPEN;

        Bet bet = new Bet.BetBuilder()
                .id(id)
                .amount(amount)
                .odds(odds)
                .client(client)
                .event(event)
                .market(market)
                .selection(selection)
                .status(status)
                .build();

        String betString = bet.toString();

        assertTrue(betString.contains("id=" + id));
        assertTrue(betString.contains("amount=" + amount));
        assertTrue(betString.contains("odds=" + odds));
        assertTrue(betString.contains("client='" + client + "'"));
        assertTrue(betString.contains("event='" + event + "'"));
        assertTrue(betString.contains("market='" + market + "'"));
        assertTrue(betString.contains("selection='" + selection + "'"));
        assertTrue(betString.contains("status=" + status));
    }
}
