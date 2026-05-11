package stocktraderproapp;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class SymbolColorManager {

    private final Map<String, String> colorBySymbol = new HashMap<>();
    private final Random random = new Random();

    public String getColorForSymbol(String symbol) {
        return colorBySymbol.computeIfAbsent(symbol, k -> generateRandomColor());
    }

    private String generateRandomColor() {
        int red   = 80 + random.nextInt(176);
        int green = 80 + random.nextInt(176);
        int blue  = 80 + random.nextInt(176);
        return String.format("#%02x%02x%02x", red, green, blue);
    }
}
