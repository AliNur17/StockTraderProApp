package stocktraderproapp;

import org.junit.After;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;

public class WatchlistManagerTest {

    @After
    public void cleanUp() {
        WatchlistManager.removeSymbol("NFLX");
        WatchlistManager.removeSymbol("AAPL");
        WatchlistManager.removeSymbol("GOOGL");
    }

    @Test
    public void addSymbol_appearsInGetSymbols() {
        WatchlistManager.addSymbol("NFLX");
        assertTrue(WatchlistManager.getSymbols().contains("NFLX"));
    }

    @Test
    public void removeSymbol_disappearsFromGetSymbols() {
        WatchlistManager.addSymbol("NFLX");
        WatchlistManager.removeSymbol("NFLX");
        assertFalse(WatchlistManager.getSymbols().contains("NFLX"));
    }

    @Test
    public void contains_returnsTrueForAddedSymbol() {
        WatchlistManager.addSymbol("NFLX");
        assertTrue(WatchlistManager.contains("NFLX"));
    }

    @Test
    public void contains_returnsFalseForNonExistentSymbol() {
        assertFalse(WatchlistManager.contains("ZZZZZ"));
    }

    @Test
    public void addSymbol_isCaseInsensitive() {
        WatchlistManager.addSymbol("aapl");
        assertTrue(WatchlistManager.contains("AAPL"));
    }

}
