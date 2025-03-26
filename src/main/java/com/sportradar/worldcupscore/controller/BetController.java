package com.sportradar.worldcupscore.controller;

import com.sportradar.worldcupscore.model.Bet;
import com.sportradar.worldcupscore.service.BetProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class BetController {
    private final BetProcessor betProcessor;

    @Autowired
    public BetController(BetProcessor betProcessor) {
        this.betProcessor = betProcessor;
    }

    @PostMapping("/bets")
    public ResponseEntity<Bet> addBet(@RequestBody Bet bet) {
        betProcessor.addBet(bet);
        return ResponseEntity.status(HttpStatus.CREATED).body(bet);
    }

    @GetMapping("/summary")
    public ResponseEntity<String> getSummary() {
        return ResponseEntity.ok(betProcessor.getSummary());
    }

    @GetMapping("/bets/review")
    public ResponseEntity<List<Bet>> getReviewBets() {
        List<Bet> reviewBets = betProcessor.getReviewBets();
        return ResponseEntity.ok(reviewBets);
    }

    @PostMapping("/shutdown")
    public ResponseEntity<String> shutdownSystem() {
        betProcessor.shutdownSystem();
        return ResponseEntity.ok("System shutdown initiated.");
    }


}
