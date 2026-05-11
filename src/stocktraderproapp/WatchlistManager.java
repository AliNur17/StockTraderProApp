package stocktraderproapp;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashSet;
import java.util.Set;

// constructs list for users watchlist (watchlist.txt)
public class WatchlistManager {

    private static final Path WATCHLIST_FILE = Path.of("watchlist.txt");

    private WatchlistManager() {}

    public static Set<String> getSymbols() {

        Set<String> symbols = new LinkedHashSet<>();

        if (!Files.exists(WATCHLIST_FILE)) {
            return symbols;
        }

        try (BufferedReader reader = Files.newBufferedReader(WATCHLIST_FILE)) {

            String line;

            while ((line = reader.readLine()) != null) {

                String symbol = line.trim().toUpperCase();

                if (!symbol.isEmpty()) {
                    symbols.add(symbol);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return symbols;
    }

    public static void addSymbol(String symbol) {
        Set<String> symbols = getSymbols();
        symbols.add(symbol.toUpperCase());
        saveSymbols(symbols);
    }

    public static void removeSymbol(String symbol) {
        Set<String> symbols = getSymbols();
        symbols.remove(symbol.toUpperCase());
        saveSymbols(symbols);
    }

    public static boolean contains(String symbol) {
        return getSymbols().contains(symbol.toUpperCase());
    }

    private static void saveSymbols(Set<String> symbols) {

        try {

            StringBuilder content = new StringBuilder();

            for (String s : symbols) {
                content.append(s).append("\n");
            }

            Files.writeString(
                    WATCHLIST_FILE,
                    content.toString(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
