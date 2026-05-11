package stocktraderproapp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Portfolio {
    private double currentBalance;
    private Map<String, Double> holdings; // stock, # of shares
    private Map<String, Double> currentPricePerShare; 
    private List<Trade> tradeHistory;
    private List<Trade> tradeQueue; // unused in current iteration

    public Portfolio(Map<String, Double> startingPrices) {
        currentBalance = 10000;
        holdings = new HashMap<>();
        currentPricePerShare = new HashMap<>();
        tradeHistory = new ArrayList<>();
        tradeQueue = new ArrayList<>();
        for (Map.Entry<String, Double> curr : startingPrices.entrySet()) { 
            currentPricePerShare.put(curr.getKey(), curr.getValue());
        }
    }

    public void updatePrices(Map<String, Double> updatedPrices) {
        for (Map.Entry<String, Double> curr : updatedPrices.entrySet()) {
            currentPricePerShare.put(curr.getKey(), curr.getValue());
        }
    }

    // also not currently used, also not tested
    private void executeTrades(List<Trade> tradeList) {
        List<Trade> toRemove = new ArrayList<>();
        for (Trade t : tradeQueue) {
            int a = makeTrade(t);
            if (a == 0) {
                toRemove.add(t);
            }
        }
        for (Trade t : tradeList) {
            int a = makeTrade(t);
            if (a == -1) {
                tradeQueue.add(t);
            }
        }
        for (Trade t : toRemove) {
            tradeQueue.remove(t);
        }
    }

    public int makeTrade(Trade t) { // returns -1 for insufficient balance, 0 for trade went thru
        if (t.getBuyOrSell().equalsIgnoreCase("buy")) {
            if (currentBalance < t.getPriceAmount()) {
                return -1;
            }
            currentBalance -= t.getPriceAmount();
            if (holdings.containsKey(t.getStock())) {
                double currentHoldings = holdings.get(t.getStock());
                currentHoldings += t.getShareAmount();
                holdings.put(t.getStock(), currentHoldings);
            }
            else {
                holdings.put(t.getStock(), t.getShareAmount());
            }
            tradeHistory.add(t);
            return 0;
        }
        else if (t.getBuyOrSell().equalsIgnoreCase("sell")) {
            if (!holdings.containsKey(t.getStock()) || holdings.get(t.getStock()) + 0.0001 < t.getShareAmount()) {
                return -1;
            }
            currentBalance += t.getPriceAmount();
            if (Math.abs(t.getShareAmount() - holdings.get(t.getStock())) < 0.0001) { // havent tested if == works, but to avoid floating point errors
                holdings.remove(t.getStock());
            }
            else {
                double currentHoldings = holdings.get(t.getStock());
                currentHoldings -= t.getShareAmount();
                holdings.put(t.getStock(), currentHoldings);
            }
            tradeHistory.add(t);
            return 0;
        }
        return -1;
    }

    public double getTotalBalance() {
        double totalBalance = currentBalance;
        for (Map.Entry<String, Double> curr : holdings.entrySet()) {
            String stock = curr.getKey();
            double shares = curr.getValue();
            if (currentPricePerShare.containsKey(stock)) {
                totalBalance += shares * currentPricePerShare.get(stock);
            }
        }
        return totalBalance;
    }

    public double getCurrentBalance() {
        return currentBalance;
    }

    public double getHoldings(String s) {
        if (holdings.containsKey(s)) {
            return holdings.get(s);
        }
        return 0;
    }
}
