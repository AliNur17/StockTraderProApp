package stocktraderproapp;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

public class TradingCalendar {

    private static final Set<LocalDate> MARKET_HOLIDAYS =
            Set.of(
                    LocalDate.of(2026, 4, 3)
            );

    private TradingCalendar() {}

    public static boolean isTradingDate(LocalDate date) {

        DayOfWeek day = date.getDayOfWeek();

        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return false;
        }

        return !MARKET_HOLIDAYS.contains(date);
    }

    public static Set<LocalDate> buildRequiredTradingDates(
            LocalDate startDate,
            LocalDate endDate) {

        Set<LocalDate> requiredDates = new HashSet<>();
        LocalDate date = startDate;

        while (!date.isAfter(endDate)) {
            if (isTradingDate(date)) {
                requiredDates.add(date);
            }
            date = date.plusDays(1);
        }

        return requiredDates;
    }
}
