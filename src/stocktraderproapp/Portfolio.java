package stocktraderproapp;

import java.util.HashMap;
import java.util.Map;

public class Portfolio {

    private double cash;
    private final Map<String, Integer> sharesOwned = new HashMap<>();

    public Portfolio(double startingCash) {
        this.cash = startingCash;
    }

    public boolean buy(String symbol, double price) {
        if (price > cash) {
            return false;
        }
        cash -= price;
        sharesOwned.merge(symbol, 1, Integer::sum);
        return true;
    }

    public boolean sell(String symbol, double price) {
        int owned = sharesOwned.getOrDefault(symbol, 0);
        if (owned <= 0) {
            return false;
        }
        cash += price;
        sharesOwned.put(symbol, owned - 1);
        return true;
    }

    public double getTotalValue(Map<String, Double> currentPrices) {
        double total = cash;
        for (Map.Entry<String, Integer> entry : sharesOwned.entrySet()) {
            total += entry.getValue()
                    * currentPrices.getOrDefault(entry.getKey(), 0.0);
        }
        return total;
    }

    public double getCash() {
        return cash;
    }

    public int getShares(String symbol) {
        return sharesOwned.getOrDefault(symbol, 0);
    }
}
