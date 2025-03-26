package com.sportradar.worldcupscore.exception;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    @Test
    void testHandleAllExceptions() {
        GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();
        Exception ex = new Exception("Test error occurred");
        WebRequest request = Mockito.mock(WebRequest.class);
        String requestDescription = "uri=/test";
        Mockito.when(request.getDescription(false)).thenReturn(requestDescription);

        ResponseEntity<ErrorDetails> responseEntity = exceptionHandler.handleAllExceptions(ex, request);

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());

        ErrorDetails errorDetails = responseEntity.getBody();
        assertNotNull(errorDetails);
        assertNotNull(errorDetails.getTimestamp());
        assertEquals("Test error occurred", errorDetails.getMessage());
        assertEquals(requestDescription, errorDetails.getDetails());
    }
}
