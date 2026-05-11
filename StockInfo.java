package stocktraderproapp;

import java.nio.file.Path;
import java.util.Map;

public class StockInfo {

    public static final Path STOCK_DATA_FILE =
            Path.of("stock_closing_prices_may5_2026_to_100_days_prior.txt");

    public static final Map<String, String> COMPANY_NAMES =
            Map.ofEntries(
                    Map.entry("AAPL", "Apple"),
                    Map.entry("MSFT", "Microsoft"),
                    Map.entry("NVDA", "NVIDIA"),
                    Map.entry("AMZN", "Amazon"),
                    Map.entry("GOOGL", "Alphabet"),
                    Map.entry("META", "Meta"),
                    Map.entry("TSLA", "Tesla"),
                    Map.entry("AVGO", "Broadcom"),
                    Map.entry("JPM", "JPMorgan Chase"),
                    Map.entry("LLY", "Eli Lilly"),
                    Map.entry("QQQ", "Invesco QQQ ETF"),
                    Map.entry("SPY", "SPDR S&P 500 ETF")
            );

    private StockInfo() {}
}
