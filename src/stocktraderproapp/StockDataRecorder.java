package stocktraderproapp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javafx.concurrent.Task;

import org.json.JSONObject;

public class StockDataRecorder {

    private static final String API_KEY = "DPTNWD8PD2WLDQ0E";

    /*
     * Includes 10 major stocks, plus NASDAQ and S&P 500.
     *
     * QQQ represents the NASDAQ-100.
     * SPY represents the S&P 500.
     */
    private static final String[] SYMBOLS_TO_RECORD = {
            "AAPL",
            "MSFT",
            "NVDA",
            "AMZN",
            "GOOGL",
            "META",
            "TSLA",
            "AVGO",
            "JPM",
            "LLY",
            "QQQ",
            "SPY"
    };

    /*
     * May 5, 2026 to 100 days before.
     * This gives January 25, 2026 through May 5, 2026.
     */
    private static final LocalDate END_DATE =
            LocalDate.of(2026, 5, 5);

    private static final LocalDate START_DATE =
            END_DATE.minusDays(100);

    private static final Path OUTPUT_FILE =
            Path.of("stock_closing_prices_may5_2026_to_100_days_prior.txt");

    /*
     * NYSE/Nasdaq market holidays that fall inside this date range.
     * Weekends are already skipped separately.
     */
    private static final Set<LocalDate> MARKET_HOLIDAYS =
            Set.of(
                    LocalDate.of(2026, 4, 3)
            );

    private static boolean isRunning = false;

    private StockDataRecorder() {
        /*
         * Utility class. Do not instantiate.
         */
    }

    public static synchronized void startRecordingInBackground() {

        if (isRunning) {
            return;
        }

        isRunning = true;

        Task<Void> task =
                new Task<>() {

            @Override
            protected Void call() throws Exception {

                try {
                    recordStockData();
                } finally {
                    isRunning = false;
                }

                return null;
            }
        };

        task.setOnFailed(event -> {
            isRunning = false;

            Throwable error =
                    task.getException();

            if (error != null) {
                error.printStackTrace();
            }
        });

        Thread thread =
                new Thread(task);

        thread.setDaemon(true);
        thread.start();
    }

    private static void recordStockData()
            throws Exception {

        ensureOutputFileExists();

        Map<String, Set<LocalDate>> existingDatesBySymbol =
                loadExistingDatesBySymbol();

        Set<LocalDate> requiredTradingDates =
                buildRequiredTradingDates();

        for (String symbol : SYMBOLS_TO_RECORD) {

            Set<LocalDate> existingDatesForSymbol =
                    existingDatesBySymbol.getOrDefault(
                            symbol,
                            new HashSet<>()
                    );

            Set<LocalDate> missingDates =
                    findMissingDates(
                            requiredTradingDates,
                            existingDatesForSymbol
                    );

            /*
             * Important:
             * If this symbol already has every required date in the txt file,
             * do not query Alpha Vantage at all.
             */
            if (missingDates.isEmpty()) {
                continue;
            }

            /*
             * Only reaches this point when the txt file is missing
             * at least one required date for this symbol.
             */
            JSONObject timeSeries =
                    fetchDailyTimeSeries(symbol);

            boolean addedAnyRecord =
                    false;

            for (String dateText : timeSeries.keySet()) {

                LocalDate date =
                        LocalDate.parse(dateText);

                if (!missingDates.contains(date)) {
                    continue;
                }

                JSONObject dailyData =
                        timeSeries.getJSONObject(dateText);

                String close =
                        dailyData.getString("4. close");

                String record =
                        symbol + ","
                        + dateText + ","
                        + close;

                appendRecord(record);

                existingDatesForSymbol.add(date);
                addedAnyRecord = true;
            }

            existingDatesBySymbol.put(
                    symbol,
                    existingDatesForSymbol
            );

            /*
             * Only sleep after an actual API query.
             * If the symbol was skipped because it already existed,
             * there is no delay.
             */
            if (addedAnyRecord) {
                Thread.sleep(15000);
            }
        }
    }

    private static Set<LocalDate> findMissingDates(
            Set<LocalDate> requiredTradingDates,
            Set<LocalDate> existingDatesForSymbol) {

        Set<LocalDate> missingDates =
                new HashSet<>();

        for (LocalDate date : requiredTradingDates) {

            if (!existingDatesForSymbol.contains(date)) {
                missingDates.add(date);
            }
        }

        return missingDates;
    }

    private static Set<LocalDate> buildRequiredTradingDates() {

        Set<LocalDate> requiredDates =
                new HashSet<>();

        LocalDate date =
                START_DATE;

        while (!date.isAfter(END_DATE)) {

            if (isTradingDate(date)) {
                requiredDates.add(date);
            }

            date =
                    date.plusDays(1);
        }

        return requiredDates;
    }

    private static boolean isTradingDate(LocalDate date) {

        DayOfWeek day =
                date.getDayOfWeek();

        if (day == DayOfWeek.SATURDAY
                || day == DayOfWeek.SUNDAY) {
            return false;
        }

        return !MARKET_HOLIDAYS.contains(date);
    }

    private static JSONObject fetchDailyTimeSeries(String symbol)
            throws Exception {

        String urlString =
                "https://www.alphavantage.co/query?"
                + "function=TIME_SERIES_DAILY"
                + "&symbol=" + symbol
                + "&outputsize=compact"
                + "&apikey=" + API_KEY;

        URL url =
                new URL(urlString);

        HttpURLConnection connection =
                (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("GET");

        BufferedReader reader =
                new BufferedReader(
                        new InputStreamReader(
                                connection.getInputStream()
                        )
                );

        StringBuilder response =
                new StringBuilder();

        String line;

        while ((line = reader.readLine()) != null) {
            response.append(line);
        }

        reader.close();

        JSONObject json =
                new JSONObject(response.toString());

        if (!json.has("Time Series (Daily)")) {

            if (json.has("Information")) {
                throw new Exception(
                        json.getString("Information")
                );
            }

            if (json.has("Note")) {
                throw new Exception(
                        json.getString("Note")
                );
            }

            throw new Exception(
                    "No stock data found for "
                    + symbol
                    + ". Check the symbol, API key, or request limit."
            );
        }

        return json.getJSONObject("Time Series (Daily)");
    }

    private static void ensureOutputFileExists()
            throws IOException {

        if (!Files.exists(OUTPUT_FILE)) {

            Files.writeString(
                    OUTPUT_FILE,
                    "Symbol,Date,Close\n",
                    StandardOpenOption.CREATE
            );
        }
    }

    private static Map<String, Set<LocalDate>> loadExistingDatesBySymbol()
            throws IOException {

        Map<String, Set<LocalDate>> existingDatesBySymbol =
                new HashMap<>();

        ensureOutputFileExists();

        try (BufferedReader reader =
                     Files.newBufferedReader(OUTPUT_FILE)) {

            String line;

            while ((line = reader.readLine()) != null) {

                ExistingRecord existingRecord =
                        parseExistingRecord(line);

                if (existingRecord == null) {
                    continue;
                }

                existingDatesBySymbol
                        .computeIfAbsent(
                                existingRecord.getSymbol(),
                                key -> new HashSet<>()
                        )
                        .add(existingRecord.getDate());
            }
        }

        return existingDatesBySymbol;
    }

    private static ExistingRecord parseExistingRecord(String line) {

        if (line == null) {
            return null;
        }

        line =
                line.trim();

        if (line.isEmpty()
                || line.equalsIgnoreCase("Symbol,Date,Close")) {
            return null;
        }

        String[] parts =
                line.split(",");

        if (parts.length < 3) {
            return null;
        }

        try {

            String symbol =
                    parts[0].trim().toUpperCase();

            LocalDate date =
                    LocalDate.parse(parts[1].trim());

            return new ExistingRecord(
                    symbol,
                    date
            );

        } catch (Exception e) {

            return null;
        }
    }

    private static void appendRecord(String record)
            throws IOException {

        try (BufferedWriter writer =
                     Files.newBufferedWriter(
                             OUTPUT_FILE,
                             StandardOpenOption.CREATE,
                             StandardOpenOption.APPEND
                     )) {

            writer.write(record);
            writer.newLine();
        }
    }

    private static class ExistingRecord {

        private final String symbol;
        private final LocalDate date;

        public ExistingRecord(
                String symbol,
                LocalDate date) {

            this.symbol =
                    symbol;

            this.date =
                    date;
        }

        public String getSymbol() {
            return symbol;
        }

        public LocalDate getDate() {
            return date;
        }
    }
}
