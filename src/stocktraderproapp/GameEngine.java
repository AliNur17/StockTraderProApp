package stocktraderproapp;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class GameEngine {
    ArrayList<StockRecord> records;
    LocalDate currentDate;
    LocalDate firstDate;
    LocalDate lastDate;
    int index;
    Portfolio playerPortfolio;

    public GameEngine(ArrayList<StockRecord> stockRecords) {
        // note ! this takes all stock records i think theoreitaclly we should only want wishlisted stock options
        // to solve this, either pass me stock records of only wishlists, or prune the stock record here
        records = new ArrayList<>(stockRecords);
        Collections.sort(records); // prob not necessary but good to do
        firstDate = records.get(0).getDate();
        currentDate = records.get(0).getDate();
        lastDate = records.get(records.size() - 1).getDate();
        index = 0;
        HashMap<String, Double> startingPrices = new HashMap<>(); 
        for (StockRecord curr : stockRecords) {
            if (curr.getDate().equals(firstDate)) {
                startingPrices.put(curr.getSymbol(), curr.getClose());
            }
        }
        playerPortfolio = new Portfolio(startingPrices);
        // this is necessary to hide later dates from portfolio
        // while it may not have much of an impact on our current iteration,
        // it may affect other strategies if implemented
    }

    // returns -1 if the date becomes the final date, meaning no more next dates
    // returns 0 otherwise
    public int nextDay() {
        index++;
        currentDate = records.get(index).getDate();
        if (currentDate.compareTo(lastDate) == 0) {
            return -1;
        }
        return 0;
    }

    // current getters return the data type. if its better for the GUI, you can change them to send strings instead
    public int makeTrade(Trade t) {
        return playerPortfolio.makeTrade(t);
    }

    public LocalDate getCurrentDate() {
        return currentDate;
    }

    public LocalDate getFirstDate() {
        return firstDate;
    }

    public LocalDate getLastDate() {
        return lastDate;
    }

    public double getCurrentBalance() {
        return playerPortfolio.getCurrentBalance();
    }

    public double getTotalBalance() {
        return playerPortfolio.getTotalBalance();
    }

    public double getHoldings(String s) { // s here represents the stock
        return playerPortfolio.getHoldings(s);
    }

    public double getHoldingPrice(String s) { // s here represents the stock
        return playerPortfolio.getHoldingPrice(s);
    }
}
