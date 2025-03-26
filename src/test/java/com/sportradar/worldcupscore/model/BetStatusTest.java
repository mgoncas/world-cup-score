package com.sportradar.worldcupscore.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BetStatusTest {

    @Test
    void testEnumValues() {
        BetStatus[] statuses = BetStatus.values();
        assertEquals(4, statuses.length);
        assertEquals(BetStatus.OPEN, statuses[0]);
        assertEquals(BetStatus.WINNER, statuses[1]);
        assertEquals(BetStatus.LOSER, statuses[2]);
        assertEquals(BetStatus.VOID, statuses[3]);
    }

    @Test
    void testValueOf() {
        assertEquals(BetStatus.OPEN, BetStatus.valueOf("OPEN"));
        assertEquals(BetStatus.WINNER, BetStatus.valueOf("WINNER"));
        assertEquals(BetStatus.LOSER, BetStatus.valueOf("LOSER"));
        assertEquals(BetStatus.VOID, BetStatus.valueOf("VOID"));
    }
}
