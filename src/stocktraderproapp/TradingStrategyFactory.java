package stocktraderproapp;

public class TradingStrategyFactory {
    public TradingStrategy getTradingStrategy(String type) {
        if (type.equalsIgnoreCase("human")) {
            return new HumanTradingStrategy();
        } else if (type.equalsIgnoreCase("human")) {
            return new MovingAverageTradingStrategy();
        } else if (type.equalsIgnoreCase("human")) {
            return new SPYAverageInTradingStrategy();
        }
        return null;
    }
}
