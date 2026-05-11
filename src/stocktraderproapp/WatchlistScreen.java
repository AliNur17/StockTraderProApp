package stocktraderproapp;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

// makes screen for watchlist based on watchlist.txt that shows the stocks, price, etc.
public class WatchlistScreen extends BaseScreen {

    public WatchlistScreen(ScreenManager manager) {
        super(manager);
    }

    @Override
    public Parent getView() {

        Label title = new Label("My Watchlist");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");

        VBox listBox = new VBox(10);
        listBox.setPadding(new Insets(10));

        Set<String> symbols = WatchlistManager.getSymbols();
        Map<String, double[]> priceRanges = loadPriceRanges(symbols);

        if (symbols.isEmpty()) {

            Label empty = new Label(
                    "Your watchlist is empty.\nAdd stocks from the Search screen."
            );
            empty.setStyle("-fx-text-fill: #888888;");
            empty.setWrapText(true);
            listBox.getChildren().add(empty);

        } else {

            for (String symbol : symbols) {
                listBox.getChildren().add(
                        buildRow(symbol, priceRanges.get(symbol))
                );
            }
        }

        ScrollPane scrollPane = new ScrollPane(listBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        Button backButton = new Button("Back");
        backButton.setOnAction(e -> manager.show(Main.HOME));

        VBox content = new VBox(16, title, scrollPane, backButton);
        content.setPadding(new Insets(24));
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        return content;
    }

    private HBox buildRow(String symbol, double[] range) {

        String companyName =
                StockInfo.COMPANY_NAMES.getOrDefault(symbol, "");

        Label symbolLabel = new Label(symbol);
        symbolLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label nameLabel = new Label(companyName);
        nameLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 11px;");

        VBox symbolBox = new VBox(2, symbolLabel, nameLabel);
        HBox.setHgrow(symbolBox, Priority.ALWAYS);

        Label changeLabel = new Label();

        if (range != null && range[0] != 0) {

            double pct = (range[1] - range[0]) / range[0] * 100;

            changeLabel.setText(
                    String.format("%s%.1f%%", pct >= 0 ? "+" : "", pct)
            );

            changeLabel.setStyle(
                    pct >= 0
                            ? "-fx-text-fill: #2e7d32; -fx-font-weight: bold;"
                            : "-fx-text-fill: #c62828; -fx-font-weight: bold;"
            );
        }

        Button removeButton = new Button("Remove");
        removeButton.setOnAction(e -> {
            WatchlistManager.removeSymbol(symbol);
            manager.show(Main.WATCHLIST);
        });

        HBox row = new HBox(12, symbolBox, changeLabel, removeButton);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10));
        row.setStyle(
                "-fx-background-color: #f5f5f5;"
                        + "-fx-border-color: #dddddd;"
                        + "-fx-border-width: 1;"
                        + "-fx-border-radius: 4;"
                        + "-fx-background-radius: 4;"
        );

        return row;
    }

    private Map<String, double[]> loadPriceRanges(Set<String> symbols) {

        StockDataFileManager fileManager =
                new StockDataFileManager(StockInfo.STOCK_DATA_FILE);

        Map<String, double[]> result = new HashMap<>();

        try {

            Map<String, List<StockRecord>> allRecords =
                    fileManager.loadAllRecords();

            for (Map.Entry<String, List<StockRecord>> entry : allRecords.entrySet()) {

                if (!symbols.contains(entry.getKey())) {
                    continue;
                }

                List<StockRecord> records = entry.getValue();

                if (records.size() >= 2) {
                    result.put(
                            entry.getKey(),
                            new double[]{
                                    records.get(0).getClose(),
                                    records.get(records.size() - 1).getClose()
                            }
                    );
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }
}
