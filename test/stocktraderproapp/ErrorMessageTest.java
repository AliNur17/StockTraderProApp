package stocktraderproapp;

import org.junit.Test;

import static org.junit.Assert.*;

public class ErrorMessageTest {

    @Test
    public void rateLimitError_returnsRateLimitMessage() {
        String raw = "Thank you for using Alpha Vantage! "
                + "Our standard API rate limit is 25 requests per day.";
        String result = StockSearchScreen.formatErrorMessage("NFLX", raw);
        assertEquals("API rate limit reached. Try again tomorrow.", result);
    }

    @Test
    public void rateLimitKeyword_returnsRateLimitMessage() {
        String result = StockSearchScreen.formatErrorMessage("NFLX", "API rate limit exceeded.");
        assertEquals("API rate limit reached. Try again tomorrow.", result);
    }

    @Test
    public void noStockDataError_returnsNotFoundMessage() {
        String raw = "No stock data found for FAKE. Check the symbol, API key, or request limit.";
        String result = StockSearchScreen.formatErrorMessage("FAKE", raw);
        assertEquals("\"FAKE\" not found. Check the ticker.", result);
    }

    @Test
    public void networkError_returnsNetworkMessage() {
        String result = StockSearchScreen.formatErrorMessage("AAPL", "Connection refused");
        assertEquals("Network error. Check your connection.", result);
    }

    @Test
    public void nullError_returnsNetworkMessage() {
        String result = StockSearchScreen.formatErrorMessage("AAPL", null);
        assertEquals("Network error. Check your connection.", result);
    }
}
