package stocktraderproapp;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.util.StringConverter;

public class StockSearchScreen extends BaseScreen {

    private final Map<String, List<StockRecord>> stockDataBySymbol =
            new HashMap<>();

    private final List<String> availableSymbols =
            new ArrayList<>();

    private final Set<String> selectedOverlaySymbols =
            new LinkedHashSet<>();

    private final SymbolColorManager colorManager =
            new SymbolColorManager();

    private String currentSingleSymbol = null;
    private Button watchlistToggleButton;

    public StockSearchScreen(ScreenManager manager) {
        super(manager);
        loadAllStockDataFromTxt();
    }

    @Override
    public Parent getView() {

        StockDataRecorder.startRecordingInBackground();

        NumberAxis xAxis =
                new NumberAxis();

        NumberAxis yAxis =
                new NumberAxis();

        xAxis.setLabel("Date");
        yAxis.setLabel("Closing Price ($)");

        xAxis.setAutoRanging(false);
        yAxis.setAutoRanging(false);

        LineChart<Number, Number> chart =
                new LineChart<>(xAxis, yAxis);

        chart.setTitle("Select a Stock");
        chart.setCreateSymbols(false);
        chart.setAnimated(false);
        chart.setLegendVisible(false);
        chart.setPadding(new Insets(20, 40, 10, 40));

        Pane hoverPane =
                new ChartHoverOverlay(chart, xAxis, yAxis).getPane();

        StackPane chartStack =
                new StackPane(
                        chart,
                        hoverPane
                );

        VBox chartArea =
                new VBox(chartStack);

        VBox.setVgrow(chartStack, Priority.ALWAYS);

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

        ProgressIndicator loadingSpinner = new ProgressIndicator();
        loadingSpinner.setMaxSize(22, 22);
        loadingSpinner.setVisible(false);

        watchlistToggleButton = new Button("Add to Watchlist");
        watchlistToggleButton.setMaxWidth(Double.MAX_VALUE);
        watchlistToggleButton.setDisable(true);
        watchlistToggleButton.setOnAction(e -> {
            if (currentSingleSymbol == null) return;
            if (WatchlistManager.contains(currentSingleSymbol)) {
                WatchlistManager.removeSymbol(currentSingleSymbol);
            } else {
                WatchlistManager.addSymbol(currentSingleSymbol);
            }
            refreshWatchlistButton();
        });

        Button viewWatchlistButton = new Button("View Watchlist");
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

        Runnable refreshButtons =
                () -> refreshStockButtons(
                        stockButtonBox,
                        stockSearchField.getText(),
                        chart,
                        xAxis,
                        yAxis,
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

                    updateChart(
                            chart,
                            xAxis,
                            yAxis,
                            overlayCheckBox
                    );

                    refreshButtons.run();
                }
        );


        BorderPane root =
                new BorderPane();

        root.setLeft(leftBar);
        root.setCenter(chartArea);

        return root;
    }

    private void refreshStockButtons(
            VBox stockButtonBox,
            String filter,
            LineChart<Number, Number> chart,
            NumberAxis xAxis,
            NumberAxis yAxis,
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

                updateChart(
                        chart,
                        xAxis,
                        yAxis,
                        overlayCheckBox
                );

                refreshStockButtons(
                        stockButtonBox,
                        filter,
                        chart,
                        xAxis,
                        yAxis,
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

    private void updateChart(
            LineChart<Number, Number> chart,
            NumberAxis xAxis,
            NumberAxis yAxis,
            CheckBox overlayCheckBox) {

        chart.getData().clear();

        List<String> symbolsToDisplay =
                getSymbolsToDisplay(overlayCheckBox);

        if (symbolsToDisplay.isEmpty()) {
            chart.setTitle("Select a Stock");
            return;
        }

        List<LocalDate> axisDates =
                buildAxisDatesForSymbols(symbolsToDisplay);

        if (axisDates.isEmpty()) {
            chart.setTitle("No saved TXT data found");
            return;
        }

        Map<LocalDate, Integer> dateIndexMap =
                buildDateIndexMap(axisDates);

        List<String> xLabels =
                buildDateLabels(axisDates);

        double[] yRange =
                addSeriesToChart(
                        chart,
                        symbolsToDisplay,
                        dateIndexMap
                );

        if (chart.getData().isEmpty()) {
            chart.setTitle("No saved TXT data found for selected stock");
            return;
        }

        configureXAxis(
                xAxis,
                axisDates,
                xLabels
        );

        configureYAxis(
                yAxis,
                yRange[0],
                yRange[1]
        );

        if (overlayCheckBox.isSelected()) {
            chart.setTitle("Overlayed Closing Prices from TXT File");
        } else {
            chart.setTitle(
                    symbolsToDisplay.get(0)
                            + " Closing Prices from TXT File"
            );
        }

        Platform.runLater(() -> applyStableSeriesColors(chart));
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

    private Map<LocalDate, Integer> buildDateIndexMap(
            List<LocalDate> axisDates) {

        Map<LocalDate, Integer> dateIndexMap =
                new HashMap<>();

        for (int i = 0; i < axisDates.size(); i++) {
            dateIndexMap.put(axisDates.get(i), i);
        }

        return dateIndexMap;
    }

    private List<String> buildDateLabels(
            List<LocalDate> axisDates) {

        List<String> xLabels =
                new ArrayList<>();

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("MMM dd");

        for (LocalDate date : axisDates) {
            xLabels.add(date.format(formatter));
        }

        return xLabels;
    }

    private double[] addSeriesToChart(
            LineChart<Number, Number> chart,
            List<String> symbolsToDisplay,
            Map<LocalDate, Integer> dateIndexMap) {

        double minClose =
                Double.MAX_VALUE;

        double maxClose =
                0;

        for (String symbol : symbolsToDisplay) {

            List<StockRecord> records =
                    stockDataBySymbol.get(symbol);

            if (records == null || records.isEmpty()) {
                continue;
            }

            XYChart.Series<Number, Number> series =
                    new XYChart.Series<>();

            series.setName(symbol);

            for (StockRecord record : records) {

                Integer xIndex =
                        dateIndexMap.get(record.getDate());

                if (xIndex == null) {
                    continue;
                }

                XYChart.Data<Number, Number> dataPoint =
                        new XYChart.Data<>(
                                xIndex,
                                record.getClose()
                        );

                dataPoint.setExtraValue(record);

                series.getData().add(dataPoint);

                minClose =
                        Math.min(
                                minClose,
                                record.getClose()
                        );

                maxClose =
                        Math.max(
                                maxClose,
                                record.getClose()
                        );
            }

            chart.getData().add(series);
        }

        if (minClose == Double.MAX_VALUE) {
            minClose = 0;
        }

        return new double[] {
                minClose,
                maxClose
        };
    }

    private void configureXAxis(
            NumberAxis xAxis,
            List<LocalDate> axisDates,
            List<String> xLabels) {

        int finalDayIndex =
                axisDates.size();

        xAxis.setLowerBound(0);
        xAxis.setUpperBound(Math.max(0, finalDayIndex - 1));
        xAxis.setTickUnit(Math.max(1, finalDayIndex / 10));

        xAxis.setTickLabelFormatter(
                new StringConverter<Number>() {

                    @Override
                    public String toString(Number value) {

                        int index =
                                value.intValue();

                        if (index >= 0
                                && index < xLabels.size()) {

                            return xLabels.get(index);
                        }

                        return "";
                    }

                    @Override
                    public Number fromString(String string) {
                        return 0;
                    }
                }
        );
    }

    private void configureYAxis(
            NumberAxis yAxis,
            double minClose,
            double maxClose) {

        double yPadding =
                Math.max(
                        1,
                        (maxClose - minClose) * 0.15
                );

        yAxis.setLowerBound(
                Math.max(
                        0,
                        minClose - yPadding
                )
        );

        yAxis.setUpperBound(
                maxClose + yPadding
        );

        yAxis.setTickUnit(
                Math.max(
                        1,
                        (maxClose - minClose) / 10
                )
        );
    }

    private List<LocalDate> buildAxisDatesForSymbols(
            List<String> symbolsToDisplay) {

        Set<LocalDate> allDates =
                new TreeSet<>();

        for (String symbol : symbolsToDisplay) {

            List<StockRecord> records =
                    stockDataBySymbol.get(symbol);

            if (records == null) {
                continue;
            }

            for (StockRecord record : records) {
                allDates.add(record.getDate());
            }
        }

        return new ArrayList<>(allDates);
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


    private void applyStableSeriesColors(
            LineChart<Number, Number> chart) {

        for (XYChart.Series<Number, Number> series
                : chart.getData()) {

            String color =
                    colorManager.getColorForSymbol(series.getName());

            Node seriesNode =
                    series.getNode();

            if (seriesNode != null) {

                Node line =
                        seriesNode.lookup(".chart-series-line");

                if (line != null) {
                    line.setStyle(
                            "-fx-stroke: "
                                    + color
                                    + ";"
                                    + "-fx-stroke-width: 2px;"
                    );
                }
            }
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
