package stocktraderproapp;

import org.junit.Test;

import java.time.LocalDate;

import static org.junit.Assert.*;

public class TradingDateTest {

    @Test
    public void saturday_isNotATradingDay() {
        // April 4, 2026 is a Saturday
        assertFalse(StockDataRecorder.isTradingDate(LocalDate.of(2026, 4, 4)));
    }

    @Test
    public void sunday_isNotATradingDay() {
        // April 5, 2026 is a Sunday
        assertFalse(StockDataRecorder.isTradingDate(LocalDate.of(2026, 4, 5)));
    }

    @Test
    public void monday_isATradingDay() {
        // April 7, 2026 is a Monday (no holiday)
        assertTrue(StockDataRecorder.isTradingDate(LocalDate.of(2026, 4, 7)));
    }

    @Test
    public void friday_isATradingDay() {
        // April 17, 2026 is a Friday (no holiday)
        assertTrue(StockDataRecorder.isTradingDate(LocalDate.of(2026, 4, 17)));
    }

    @Test
    public void goodFriday_isNotATradingDay() {
        // April 3, 2026 is Good Friday — a market holiday
        assertFalse(StockDataRecorder.isTradingDate(LocalDate.of(2026, 4, 3)));
    }
}
