package stocktraderproapp;

import org.junit.Test;

import static org.junit.Assert.*;

public class CompanyNamesTest {

    @Test
    public void aapl_mapsToApple() {
        assertEquals("Apple", WatchlistManager.COMPANY_NAMES.get("AAPL"));
    }

    @Test
    public void msft_mapsToMicrosoft() {
        assertEquals("Microsoft", WatchlistManager.COMPANY_NAMES.get("MSFT"));
    }

    @Test
    public void nvda_mapsToNvidia() {
        assertEquals("NVIDIA", WatchlistManager.COMPANY_NAMES.get("NVDA"));
    }

    @Test
    public void unknownSymbol_returnsNull() {
        assertNull(WatchlistManager.COMPANY_NAMES.get("ZZZZZ"));
    }

    @Test
    public void allTwelveSymbolsPresent() {
        assertEquals(12, WatchlistManager.COMPANY_NAMES.size());
    }
}
