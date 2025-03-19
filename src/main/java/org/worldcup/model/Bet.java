package org.worldcup.model;

public class Bet {

    private final int id;
    private final double amount;
    private final double odds;
    private final String client;
    private final String event;
    private final String market;
    private final String selection;
    private final BetStatus status;

    private Bet(BetBuilder builder) {
        this.id = builder.id;
        this.amount = builder.amount;
        this.odds = builder.odds;
        this.client = builder.client;
        this.event = builder.event;
        this.market = builder.market;
        this.selection = builder.selection;
        this.status = builder.status;
    }

    public int getId() {
        return id;
    }

    public double getAmount() {
        return amount;
    }

    public double getOdds() {
        return odds;
    }

    public String getClient() {
        return client;
    }

    public String getEvent() {
        return event;
    }

    public String getMarket() {
        return market;
    }

    public String getSelection() {
        return selection;
    }

    public BetStatus getStatus() {
        return status;
    }

    public static class BetBuilder {
        private int id;
        private double amount;
        private double odds;
        private String client;
        private String event;
        private String market;
        private String selection;
        private BetStatus status;

        public BetBuilder id(int id) {
            this.id = id;
            return this;
        }

        public BetBuilder amount(double amount) {
            this.amount = amount;
            return this;
        }

        public BetBuilder odds(double odds) {
            this.odds = odds;
            return this;
        }

        public BetBuilder client(String client) {
            this.client = client;
            return this;
        }

        public BetBuilder event(String event) {
            this.event = event;
            return this;
        }

        public BetBuilder market(String market) {
            this.market = market;
            return this;
        }

        public BetBuilder selection(String selection) {
            this.selection = selection;
            return this;
        }

        public BetBuilder status(BetStatus status) {
            this.status = status;
            return this;
        }

        public Bet build() {
            return new Bet(this);
        }
    }

    @Override
    public String toString() {
        return "Bet{" +
                "id=" + id +
                ", amount=" + amount +
                ", odds=" + odds +
                ", client='" + client + '\'' +
                ", event='" + event + '\'' +
                ", market='" + market + '\'' +
                ", selection='" + selection + '\'' +
                ", status=" + status +
                '}';
    }
}