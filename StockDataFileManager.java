package stocktraderproapp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StockDataFileManager {

    private final Path dataFile;

    public StockDataFileManager(Path dataFile) {
        this.dataFile = dataFile;
    }

    public void ensureFileExists() throws IOException {

        if (!Files.exists(dataFile)) {
            Files.writeString(
                    dataFile,
                    "Symbol,Date,Close\n",
                    StandardOpenOption.CREATE
            );
        }
    }

    public Map<String, Set<LocalDate>> loadExistingDatesBySymbol()
            throws IOException {

        Map<String, Set<LocalDate>> result = new HashMap<>();

        ensureFileExists();

        try (BufferedReader reader = Files.newBufferedReader(dataFile)) {

            String line;

            while ((line = reader.readLine()) != null) {

                StockRecord record = parseRecord(line);

                if (record == null) {
                    continue;
                }

                result.computeIfAbsent(
                        record.getSymbol(),
                        k -> new HashSet<>()
                ).add(record.getDate());
            }
        }

        return result;
    }

    public Map<String, List<StockRecord>> loadAllRecords()
            throws IOException {

        Map<String, List<StockRecord>> result = new HashMap<>();

        if (!Files.exists(dataFile)) {
            return result;
        }

        try (BufferedReader reader = Files.newBufferedReader(dataFile)) {

            String line;

            while ((line = reader.readLine()) != null) {

                StockRecord record = parseRecord(line);

                if (record == null) {
                    continue;
                }

                result.computeIfAbsent(
                        record.getSymbol(),
                        k -> new ArrayList<>()
                ).add(record);
            }
        }

        return result;
    }

    public void appendRecord(String symbol, String dateText, String close)
            throws IOException {

        try (BufferedWriter writer = Files.newBufferedWriter(
                dataFile,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND)) {

            writer.write(symbol + "," + dateText + "," + close);
            writer.newLine();
        }
    }

    StockRecord parseRecord(String line) {

        if (line == null) {
            return null;
        }

        line = line.trim();

        if (line.isEmpty() || line.equalsIgnoreCase("Symbol,Date,Close")) {
            return null;
        }

        String[] parts = line.split(",");

        if (parts.length < 3) {
            return null;
        }

        try {

            String symbol = parts[0].trim().toUpperCase();
            LocalDate date = LocalDate.parse(parts[1].trim());
            double close = Double.parseDouble(parts[2].trim());

            return new StockRecord(symbol, date, close);

        } catch (Exception e) {
            return null;
        }
    }
}
