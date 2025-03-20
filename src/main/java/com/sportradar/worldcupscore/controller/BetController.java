package com.sportradar.worldcupscore.controller;

import com.sportradar.worldcupscore.model.Bet;
import com.sportradar.worldcupscore.service.BetProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class BetController {
    private final BetProcessor betProcessor;

    @Autowired
    public BetController(BetProcessor betProcessor) {
        this.betProcessor = betProcessor;
    }

    @PostMapping("/bets")
    public String addBet(@RequestBody Bet bet) {
        betProcessor.addBet(bet);
        return "Bet added successfully.";
    }

    @GetMapping("/summary")
    public String getSummary() {
        return betProcessor.getSummary();
    }

    @PostMapping("/shutdown")
    public String shutdownSystem() {
        betProcessor.shutdownSystem();
        return "System shutdown initiated.";
    }
}
