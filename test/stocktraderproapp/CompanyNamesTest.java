package stocktraderproapp;

import org.junit.Test;

import static org.junit.Assert.*;

public class CompanyNamesTest {

    @Test
    public void aapl_mapsToApple() {
        assertEquals("Apple", StockInfo.COMPANY_NAMES.get("AAPL"));
    }

    @Test
    public void msft_mapsToMicrosoft() {
        assertEquals("Microsoft", StockInfo.COMPANY_NAMES.get("MSFT"));
    }

    @Test
    public void nvda_mapsToNvidia() {
        assertEquals("NVIDIA", StockInfo.COMPANY_NAMES.get("NVDA"));
    }

    @Test
    public void unknownSymbol_returnsNull() {
        assertNull(StockInfo.COMPANY_NAMES.get("ZZZZZ"));
    }

    @Test
    public void allTwelveSymbolsPresent() {
        assertEquals(12, StockInfo.COMPANY_NAMES.size());
    }
}
