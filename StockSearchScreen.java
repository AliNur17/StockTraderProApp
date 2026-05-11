package stocktraderproapp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class StockSearchScreen extends BaseScreen {

    private final Map<String, List<StockRecord>> stockDataBySymbol =
            new HashMap<>();

    private final List<String> availableSymbols =
            new ArrayList<>();

    private final Set<String> selectedOverlaySymbols =
            new LinkedHashSet<>();

    private final SymbolColorManager colorManager =
            new SymbolColorManager();

    private final GraphInterface graphInterface =
            new StockGraph();

    private String currentSingleSymbol = null;
    private Button watchlistToggleButton;

    public StockSearchScreen(ScreenManager manager) {
        super(manager);
        loadAllStockDataFromTxt();
    }

    @Override
    public Parent getView() {

        StockDataRecorder.startRecordingInBackground();

        TextField stockSearchField =
                new TextField();

        stockSearchField.setPromptText("Search stocks...");

        VBox stockButtonBox =
                new VBox(8);

        stockButtonBox.setPadding(new Insets(10));
        stockButtonBox.setAlignment(Pos.TOP_CENTER);

        ScrollPane stockScrollPane =
                new ScrollPane(stockButtonBox);

        stockScrollPane.setFitToWidth(true);
        stockScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        stockScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        VBox.setVgrow(stockScrollPane, Priority.ALWAYS);

        CheckBox overlayCheckBox =
                new CheckBox("Overlay Stocks");

        overlayCheckBox.setMaxWidth(Double.MAX_VALUE);

        Button backButton =
                new Button("Back");

        backButton.setMaxWidth(Double.MAX_VALUE);
        backButton.setOnAction(e -> manager.show(Main.HOME));

        Label sidebarTitle =
                new Label("Stocks");

        Label searchStatus =
                new Label();

        searchStatus.setWrapText(true);
        searchStatus.setMaxWidth(Double.MAX_VALUE);
        searchStatus.setStyle("-fx-font-size: 10px;");

        ProgressIndicator loadingSpinner =
                new ProgressIndicator();

        loadingSpinner.setMaxSize(22, 22);
        loadingSpinner.setVisible(false);

        watchlistToggleButton =
                new Button("Add to Watchlist");

        watchlistToggleButton.setMaxWidth(Double.MAX_VALUE);
        watchlistToggleButton.setDisable(true);

        watchlistToggleButton.setOnAction(e -> {

            if (currentSingleSymbol == null) {
                return;
            }

            if (WatchlistManager.contains(currentSingleSymbol)) {
                WatchlistManager.removeSymbol(currentSingleSymbol);
            } else {
                WatchlistManager.addSymbol(currentSingleSymbol);
            }

            refreshWatchlistButton();
        });

        Button viewWatchlistButton =
                new Button("View Watchlist");

        viewWatchlistButton.setMaxWidth(Double.MAX_VALUE);
        viewWatchlistButton.setOnAction(e -> manager.show(Main.WATCHLIST));

        Label searchLabel =
                new Label("Search for stocks below...");

        searchLabel.setStyle(
                "-fx-text-fill: #9a9a9a;"
                        + "-fx-font-size: 11px;"
                        + "-fx-font-style: italic;"
        );

        VBox leftBar =
                new VBox(
                        8,
                        sidebarTitle,
                        loadingSpinner,
                        searchStatus,
                        searchLabel,
                        new Separator(),
                        stockSearchField,
                        stockScrollPane,
                        overlayCheckBox,
                        watchlistToggleButton,
                        viewWatchlistButton,
                        backButton
                );

        leftBar.setPadding(new Insets(15));
        leftBar.setPrefWidth(210);
        leftBar.setMinWidth(210);
        leftBar.setMaxWidth(210);

        VBox graphArea =
                new VBox(graphInterface.getView());

        VBox.setVgrow(graphInterface.getView(), Priority.ALWAYS);

        Runnable refreshButtons =
                () -> refreshStockButtons(
                        stockButtonBox,
                        stockSearchField.getText(),
                        overlayCheckBox
                );

        refreshButtons.run();

        stockSearchField.textProperty().addListener(
                (observable, oldValue, newValue) -> refreshButtons.run()
        );

        overlayCheckBox.selectedProperty().addListener(
                (observable, wasSelected, isSelected) -> {

                    if (isSelected) {

                        if (currentSingleSymbol != null) {
                            selectedOverlaySymbols.add(currentSingleSymbol);
                        }

                    } else {

                        selectedOverlaySymbols.clear();
                    }

                    updateGraph(overlayCheckBox);
                    refreshButtons.run();
                }
        );

        BorderPane root =
                new BorderPane();

        root.setLeft(leftBar);
        root.setCenter(graphArea);

        return root;
    }

    private void refreshStockButtons(
            VBox stockButtonBox,
            String filter,
            CheckBox overlayCheckBox) {

        stockButtonBox.getChildren().clear();

        String cleanedFilter =
                filter == null
                        ? ""
                        : filter.trim().toUpperCase();

        for (String symbol : availableSymbols) {

            String companyName =
                    StockInfo.COMPANY_NAMES.getOrDefault(symbol, "");

            boolean matchesSymbol =
                    symbol.contains(cleanedFilter);

            boolean matchesName =
                    companyName.toUpperCase().contains(cleanedFilter);

            if (!cleanedFilter.isEmpty()
                    && !matchesSymbol
                    && !matchesName) {
                continue;
            }

            String percentChange =
                    getPercentChangeLabel(symbol);

            String symbolLine =
                    percentChange.isEmpty()
                            ? symbol
                            : symbol + "  " + percentChange;

            String buttonLabel =
                    companyName.isEmpty()
                            ? symbolLine
                            : symbolLine + "\n" + companyName;

            Button stockButton =
                    new Button(buttonLabel);

            stockButton.setMaxWidth(Double.MAX_VALUE);

            if (isSymbolActive(symbol, overlayCheckBox)) {

                stockButton.setStyle(
                        "-fx-font-weight: bold;"
                                + "-fx-border-color: "
                                + colorManager.getColorForSymbol(symbol)
                                + ";"
                                + "-fx-border-width: 2;"
                );
            }

            stockButton.setOnAction(e -> {

                handleStockSelection(
                        symbol,
                        overlayCheckBox
                );

                updateGraph(overlayCheckBox);

                refreshStockButtons(
                        stockButtonBox,
                        filter,
                        overlayCheckBox
                );

                refreshWatchlistButton();
            });

            stockButtonBox.getChildren().add(stockButton);
        }
    }

    private boolean isSymbolActive(
            String symbol,
            CheckBox overlayCheckBox) {

        if (overlayCheckBox.isSelected()) {
            return selectedOverlaySymbols.contains(symbol);
        }

        return symbol.equals(currentSingleSymbol);
    }

    private void handleStockSelection(
            String symbol,
            CheckBox overlayCheckBox) {

        if (overlayCheckBox.isSelected()) {

            if (selectedOverlaySymbols.contains(symbol)) {
                selectedOverlaySymbols.remove(symbol);
            } else {
                selectedOverlaySymbols.add(symbol);
            }

            currentSingleSymbol = symbol;

        } else {

            selectedOverlaySymbols.clear();
            currentSingleSymbol = symbol;
        }
    }

    private void updateGraph(CheckBox overlayCheckBox) {

        List<String> symbolsToDisplay =
                getSymbolsToDisplay(overlayCheckBox);

        if (symbolsToDisplay.isEmpty()) {
            graphInterface.clear("Select a Stock");
            return;
        }

        String title;

        if (overlayCheckBox.isSelected()) {
            title = "Overlayed Closing Prices from TXT File";
        } else {
            title = symbolsToDisplay.get(0)
                    + " Closing Prices from TXT File";
        }

        graphInterface.showFullRecords(
                title,
                stockDataBySymbol,
                symbolsToDisplay
        );
    }

    private List<String> getSymbolsToDisplay(
            CheckBox overlayCheckBox) {

        List<String> symbolsToDisplay =
                new ArrayList<>();

        if (overlayCheckBox.isSelected()) {

            symbolsToDisplay.addAll(selectedOverlaySymbols);

        } else if (currentSingleSymbol != null) {

            symbolsToDisplay.add(currentSingleSymbol);
        }

        return symbolsToDisplay;
    }

    private void loadAllStockDataFromTxt() {

        stockDataBySymbol.clear();
        availableSymbols.clear();

        StockDataFileManager fileManager =
                new StockDataFileManager(StockInfo.STOCK_DATA_FILE);

        try {

            Map<String, List<StockRecord>> loaded =
                    fileManager.loadAllRecords();

            stockDataBySymbol.putAll(loaded);
            availableSymbols.addAll(loaded.keySet());
            Collections.sort(availableSymbols);

            for (List<StockRecord> records : stockDataBySymbol.values()) {
                Collections.sort(records);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static String formatErrorMessage(String symbol, String rawError) {

        if (rawError != null
                && (rawError.contains("rate limit")
                || rawError.contains("25 requests"))) {
            return "API rate limit reached. Try again tomorrow.";
        }

        if (rawError != null && rawError.contains("No stock data found")) {
            return "\"" + symbol + "\" not found. Check the ticker.";
        }

        return "Network error. Check your connection.";
    }

    private void refreshWatchlistButton() {

        if (currentSingleSymbol == null) {
            watchlistToggleButton.setDisable(true);
            watchlistToggleButton.setText("Add to Watchlist");
            return;
        }

        watchlistToggleButton.setDisable(false);

        if (WatchlistManager.contains(currentSingleSymbol)) {
            watchlistToggleButton.setText("Remove from Watchlist");
        } else {
            watchlistToggleButton.setText("Add to Watchlist");
        }
    }

    private String getPercentChangeLabel(String symbol) {

        List<StockRecord> records =
                stockDataBySymbol.get(symbol);

        if (records == null || records.size() < 2) {
            return "";
        }

        double earliest =
                records.get(0).getClose();

        double latest =
                records.get(records.size() - 1).getClose();

        if (earliest == 0) {
            return "";
        }

        double pct =
                (latest - earliest) / earliest * 100;

        return String.format(
                "%s%.1f%%",
                pct >= 0 ? "+" : "",
                pct
        );
    }
}