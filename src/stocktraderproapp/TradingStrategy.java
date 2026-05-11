package stocktraderproapp;

import java.util.List;

public interface TradingStrategy {
    List<Trade> decide(); // parameters tbd
}