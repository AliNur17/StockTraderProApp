package stocktraderproapp;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javafx.concurrent.Task;

import org.json.JSONObject;

public class StockDataRecorder {

    private static final String API_KEY = "DPTNWD8PD2WLDQ0E";

    private static final String[] SYMBOLS_TO_RECORD = {
            "AAPL", "MSFT", "NVDA", "AMZN", "GOOGL",
            "META", "TSLA", "AVGO", "JPM", "LLY", "QQQ", "SPY"
    };

    private static final LocalDate END_DATE = LocalDate.now();
    private static final LocalDate START_DATE = END_DATE.minusDays(100);

    private static boolean isRunning = false;

    private StockDataRecorder() {}

    public static synchronized void startRecordingInBackground() {

        if (isRunning) {
            return;
        }

        isRunning = true;

        Task<Void> task = new Task<>() {

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
            Throwable error = task.getException();
            if (error != null) {
                error.printStackTrace();
            }
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    public static boolean fetchAndRecordSymbol(String symbol) throws Exception {

        String upperSymbol = symbol.toUpperCase();

        StockDataFileManager fileManager =
                new StockDataFileManager(StockInfo.STOCK_DATA_FILE);

        fileManager.ensureFileExists();

        Map<String, Set<LocalDate>> existingDatesBySymbol =
                fileManager.loadExistingDatesBySymbol();

        Set<LocalDate> existingDates =
                existingDatesBySymbol.getOrDefault(upperSymbol, new HashSet<>());

        Set<LocalDate> requiredDates =
                TradingCalendar.buildRequiredTradingDates(START_DATE, END_DATE);

        Set<LocalDate> missingDates = findMissingDates(requiredDates, existingDates);

        if (missingDates.isEmpty()) {
            return false;
        }

        AlphaVantageClient client = new AlphaVantageClient(API_KEY);
        JSONObject timeSeries = client.fetchDailyTimeSeries(upperSymbol);

        boolean addedAny = false;

        for (String dateText : timeSeries.keySet()) {

            LocalDate date = LocalDate.parse(dateText);

            if (!missingDates.contains(date)) {
                continue;
            }

            JSONObject dailyData = timeSeries.getJSONObject(dateText);
            String close = dailyData.getString("4. close");
            fileManager.appendRecord(upperSymbol, dateText, close);
            addedAny = true;
        }

        return addedAny;
    }

    private static void recordStockData() throws Exception {

        StockDataFileManager fileManager =
                new StockDataFileManager(StockInfo.STOCK_DATA_FILE);

        fileManager.ensureFileExists();

        Map<String, Set<LocalDate>> existingDatesBySymbol =
                fileManager.loadExistingDatesBySymbol();

        Set<LocalDate> requiredTradingDates =
                TradingCalendar.buildRequiredTradingDates(START_DATE, END_DATE);

        AlphaVantageClient client = new AlphaVantageClient(API_KEY);

        for (String symbol : SYMBOLS_TO_RECORD) {

            Set<LocalDate> existingDates =
                    existingDatesBySymbol.getOrDefault(symbol, new HashSet<>());

            Set<LocalDate> missingDates =
                    findMissingDates(requiredTradingDates, existingDates);

            if (missingDates.isEmpty()) {
                continue;
            }

            JSONObject timeSeries = client.fetchDailyTimeSeries(symbol);
            boolean addedAnyRecord = false;

            for (String dateText : timeSeries.keySet()) {

                LocalDate date = LocalDate.parse(dateText);

                if (!missingDates.contains(date)) {
                    continue;
                }

                JSONObject dailyData = timeSeries.getJSONObject(dateText);
                String close = dailyData.getString("4. close");
                fileManager.appendRecord(symbol, dateText, close);
                existingDates.add(date);
                addedAnyRecord = true;
            }

            existingDatesBySymbol.put(symbol, existingDates);

            if (addedAnyRecord) {
                Thread.sleep(15000);
            }
        }
    }

    private static Set<LocalDate> findMissingDates(
            Set<LocalDate> required,
            Set<LocalDate> existing) {

        Set<LocalDate> missing = new HashSet<>();

        for (LocalDate date : required) {
            if (!existing.contains(date)) {
                missing.add(date);
            }
        }

        return missing;
    }
}
