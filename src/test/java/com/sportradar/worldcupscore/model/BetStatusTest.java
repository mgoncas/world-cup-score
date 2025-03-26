package com.sportradar.worldcupscore.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BetStatusTest {

    @Test
    void testEnumValues() {
        // Verifica que la enum tenga exactamente 4 valores y en el orden esperado.
        BetStatus[] statuses = BetStatus.values();
        assertEquals(4, statuses.length);
        assertEquals(BetStatus.OPEN, statuses[0]);
        assertEquals(BetStatus.WINNER, statuses[1]);
        assertEquals(BetStatus.LOSER, statuses[2]);
        assertEquals(BetStatus.VOID, statuses[3]);
    }

    @Test
    void testValueOf() {
        // Verifica que valueOf retorne el valor correcto para cada string.
        assertEquals(BetStatus.OPEN, BetStatus.valueOf("OPEN"));
        assertEquals(BetStatus.WINNER, BetStatus.valueOf("WINNER"));
        assertEquals(BetStatus.LOSER, BetStatus.valueOf("LOSER"));
        assertEquals(BetStatus.VOID, BetStatus.valueOf("VOID"));
    }
}
