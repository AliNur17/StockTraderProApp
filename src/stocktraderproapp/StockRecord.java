package stocktraderproapp;

import java.time.LocalDate;

public class StockRecord implements Comparable<StockRecord> {

    private final String symbol;
    private final LocalDate date;
    private final double close;

    public StockRecord(String symbol, LocalDate date, double close) {
        this.symbol = symbol;
        this.date = date;
        this.close = close;
    }

    public String getSymbol() {
        return symbol;
    }

    public LocalDate getDate() {
        return date;
    }

    public double getClose() {
        return close;
    }

    @Override
    public int compareTo(StockRecord other) {
        return this.date.compareTo(other.date);
    }
}
