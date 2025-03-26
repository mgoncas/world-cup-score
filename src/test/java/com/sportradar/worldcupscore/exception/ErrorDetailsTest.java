package com.sportradar.worldcupscore.exception;

import org.junit.jupiter.api.Test;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ErrorDetailsTest {

    @Test
    void testErrorDetailsGetters() {
        Date now = new Date();
        String message = "An error occurred";
        String details = "Error details";

        ErrorDetails errorDetails = new ErrorDetails(now, message, details);

        assertEquals(now, errorDetails.getTimestamp());
        assertEquals(message, errorDetails.getMessage());
        assertEquals(details, errorDetails.getDetails());
    }
}
