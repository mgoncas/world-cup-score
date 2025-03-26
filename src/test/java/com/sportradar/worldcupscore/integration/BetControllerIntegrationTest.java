package com.sportradar.worldcupscore.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportradar.worldcupscore.model.Bet;
import com.sportradar.worldcupscore.model.BetStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class BetControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testAddBetOpen() throws Exception {
        Bet bet = new Bet.BetBuilder()
                .id(105)
                .amount(100.0)
                .odds(1.5)
                .client("Cliente1")
                .event("Evento")
                .market("Market1")
                .selection("Selection1")
                .status(BetStatus.OPEN)
                .build();

        mockMvc.perform(post("/api/bets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bet)))
                .andExpect(status().isCreated());
    }

    @Test
    public void testAddBetUpdateToWinner() throws Exception {
        Bet winnerBet = new Bet.BetBuilder()
                .id(2)
                .amount(100.0)
                .odds(1.5)
                .client("Cliente1")
                .event("Evento1")
                .market("Market1")
                .selection("Selection1")
                .status(BetStatus.WINNER)
                .build();

        mockMvc.perform(post("/api/bets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(winnerBet)))
                .andExpect(status().isCreated());

        Thread.sleep(500);

        mockMvc.perform(get("/api/bets/review"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    public void testAddBetUpdateToWinnerAndToLoser() throws Exception {
        Bet winnerBet = new Bet.BetBuilder()
                .id(1)
                .amount(100.0)
                .odds(1.5)
                .client("Cliente1")
                .event("Evento1")
                .market("Market1")
                .selection("Selection1")
                .status(BetStatus.WINNER)
                .build();

        mockMvc.perform(post("/api/bets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(winnerBet)))
                .andExpect(status().isCreated());

        Thread.sleep(500);

        Bet loserBet = new Bet.BetBuilder()
                .id(1)// misma apuesta, misma id
                .amount(100.0)
                .odds(1.5)
                .client("Cliente1")
                .event("Evento1")
                .market("Market1")
                .selection("Selection1")
                .status(BetStatus.LOSER)
                .build();



        mockMvc.perform(post("/api/bets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loserBet)))
                .andExpect(status().isCreated());

        Thread.sleep(500);

        String reviews = "[{\"id\":1,\"amount\":100.0,\"odds\":1.5,\"client\":\"Cliente1\",\"event\":\"Evento1\",\"market\":\"Market1\",\"selection\":\"Selection1\",\"status\":\"LOSER\"}]";

        mockMvc.perform(get("/api/bets/review"))
                .andExpect(status().isOk())
                .andExpect(content().json(reviews));
    }


    @Test
    public void testGetSummaryEndpoint() throws Exception {
        Bet bet = new Bet.BetBuilder()
                .id(101)
                .amount(100.0)
                .odds(1.5)
                .client("Cliente1")
                .event("Evento1")
                .market("Market1")
                .selection("Selection1")
                .status(BetStatus.OPEN)
                .build();

        mockMvc.perform(post("/api/bets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bet)))
                .andExpect(status().isCreated());

        Thread.sleep(200);

        mockMvc.perform(get("/api/summary"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Total bets processed: 101")));
    }

    @Test
    public void testShutdownEndpoint() throws Exception {
        mockMvc.perform(post("/api/shutdown")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("System shutdown initiated."));
    }
}
