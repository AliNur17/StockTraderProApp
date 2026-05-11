package stocktraderproapp;

import java.util.HashMap;
import java.util.List;

public class Portfolio {
    double currentBalance;
    double totalBalance;
    TradingStrategy s;
    HashMap<String, Double> holdings; // stock, # of shares
    String currentDate;
    List<Trade> tradeHistory;

    public Portfolio(String strategyType, List<String> stocksWatching, String startDate) {
        currentBalance = 10000;
        totalBalance = currentBalance;
        TradingStrategyFactory tsf = new TradingStrategyFactory();
        s = tsf.getTradingStrategy(strategyType);
        for (String s : stocksWatching) {
            holdings.put(s, (double) 0);
        }
    }

    private void calculateTotalBalance() {
        // todo, just do total balance = current balance + share price * share holdings for each share
    }

    private void executeTrades(List<Trade> tradeList) {
        // todo, executes trades listed if balance is enough and adds them to trade history
    }

    // todo : increment date, tell trading strategy to go, getters
}
