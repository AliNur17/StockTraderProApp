package stocktraderproapp;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
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

public class StockSearchScreen implements AppScreen {

    private final ScreenManager manager;

    private static final Path STOCK_DATA_FILE =
            Path.of("stock_closing_prices_may5_2026_to_100_days_prior.txt");

    private final Map<String, List<StockRecord>> stockDataBySymbol =
            new HashMap<>();

    private final List<String> availableSymbols =
            new ArrayList<>();

    private final Set<String> selectedOverlaySymbols =
            new LinkedHashSet<>();

    private final Map<String, String> colorBySymbol =
            new HashMap<>();

    private final Random random =
            new Random();

    private static final Map<String, String> COMPANY_NAMES =
            Map.ofEntries(
                    Map.entry("AAPL", "Apple"),
                    Map.entry("MSFT", "Microsoft"),
                    Map.entry("NVDA", "NVIDIA"),
                    Map.entry("AMZN", "Amazon"),
                    Map.entry("GOOGL", "Alphabet"),
                    Map.entry("META", "Meta"),
                    Map.entry("TSLA", "Tesla"),
                    Map.entry("AVGO", "Broadcom"),
                    Map.entry("JPM", "JPMorgan Chase"),
                    Map.entry("LLY", "Eli Lilly"),
                    Map.entry("QQQ", "Invesco QQQ ETF"),
                    Map.entry("SPY", "SPDR S&P 500 ETF")
            );

    private String currentSingleSymbol = null;

    public StockSearchScreen(ScreenManager manager) {
        this.manager = manager;
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
                createHoverPane(
                        chart,
                        xAxis,
                        yAxis
                );

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

        Label customSearchLabel =
                new Label("Search Any Stock");

        customSearchLabel.setStyle(
                "-fx-font-size: 11px; -fx-text-fill: #888888;"
        );

        TextField customSearchField =
                new TextField();

        customSearchField.setPromptText("Ticker (e.g. NFLX)");
        customSearchField.setMaxWidth(Double.MAX_VALUE);

        Button searchButton =
                new Button("Search");

        searchButton.setMaxWidth(Double.MAX_VALUE);

        Label searchStatus =
                new Label();

        searchStatus.setWrapText(true);
        searchStatus.setMaxWidth(Double.MAX_VALUE);
        searchStatus.setStyle("-fx-font-size: 10px;");

        VBox leftBar =
                new VBox(
                        8,
                        sidebarTitle,
                        customSearchLabel,
                        customSearchField,
                        searchButton,
                        searchStatus,
                        new Separator(),
                        stockSearchField,
                        stockScrollPane,
                        overlayCheckBox,
                        backButton
                );

        leftBar.setPadding(new Insets(15));
        leftBar.setPrefWidth(190);
        leftBar.setMinWidth(190);
        leftBar.setMaxWidth(190);

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

        customSearchField.setOnAction(e -> searchButton.fire());

        searchButton.setOnAction(e -> handleCustomSearch(
                customSearchField.getText().trim().toUpperCase(),
                searchStatus,
                chart,
                xAxis,
                yAxis,
                overlayCheckBox,
                stockButtonBox,
                stockSearchField
        ));

        BorderPane root =
                new BorderPane();

        root.setLeft(leftBar);
        root.setCenter(chartArea);

        return root;
    }

    private void handleCustomSearch(
            String symbol,
            Label statusLabel,
            LineChart<Number, Number> chart,
            NumberAxis xAxis,
            NumberAxis yAxis,
            CheckBox overlayCheckBox,
            VBox stockButtonBox,
            TextField filterField) {

        if (symbol.isEmpty()) {
            statusLabel.setText("Enter a ticker symbol.");
            return;
        }

        statusLabel.setText("Fetching " + symbol + "...");

        Task<Boolean> task =
                new Task<>() {

            @Override
            protected Boolean call() throws Exception {
                return StockDataRecorder.fetchAndRecordSymbol(symbol);
            }
        };

        task.setOnSucceeded(event -> {

            loadAllStockDataFromTxt();

            if (!stockDataBySymbol.containsKey(symbol)) {
                statusLabel.setText("No data found for " + symbol + ".");
                return;
            }

            boolean wasFetched = task.getValue();

            statusLabel.setText(
                    wasFetched
                            ? "Loaded " + symbol + "."
                            : symbol + " (cached)."
            );

            currentSingleSymbol = symbol;
            selectedOverlaySymbols.clear();

            updateChart(chart, xAxis, yAxis, overlayCheckBox);

            refreshStockButtons(
                    stockButtonBox,
                    filterField.getText(),
                    chart,
                    xAxis,
                    yAxis,
                    overlayCheckBox
            );
        });

        task.setOnFailed(event -> {

            Throwable error = task.getException();

            statusLabel.setText(
                    error != null
                            ? error.getMessage()
                            : "Failed to fetch " + symbol + "."
            );
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
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
                    COMPANY_NAMES.getOrDefault(symbol, "");

            boolean matchesSymbol =
                    symbol.contains(cleanedFilter);

            boolean matchesName =
                    companyName.toUpperCase().contains(cleanedFilter);

            if (!cleanedFilter.isEmpty()
                    && !matchesSymbol
                    && !matchesName) {
                continue;
            }

            String buttonLabel =
                    companyName.isEmpty()
                            ? symbol
                            : symbol + "\n" + companyName;

            Button stockButton =
                    new Button(buttonLabel);

            stockButton.setMaxWidth(Double.MAX_VALUE);

            if (isSymbolActive(symbol, overlayCheckBox)) {

                stockButton.setStyle(
                        "-fx-font-weight: bold;"
                                + "-fx-border-color: "
                                + getColorForSymbol(symbol)
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

        if (!Files.exists(STOCK_DATA_FILE)) {
            return;
        }

        try (BufferedReader reader =
                     Files.newBufferedReader(STOCK_DATA_FILE)) {

            String line;

            while ((line = reader.readLine()) != null) {

                StockRecord record =
                        parseStockRecord(line);

                if (record == null) {
                    continue;
                }

                stockDataBySymbol
                        .computeIfAbsent(
                                record.getSymbol(),
                                key -> new ArrayList<>()
                        )
                        .add(record);
            }

            availableSymbols.addAll(
                    stockDataBySymbol.keySet()
            );

            Collections.sort(availableSymbols);

            for (List<StockRecord> records
                    : stockDataBySymbol.values()) {

                Collections.sort(records);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private StockRecord parseStockRecord(String line) {

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

            double close =
                    Double.parseDouble(parts[2].trim());

            return new StockRecord(
                    symbol,
                    date,
                    close
            );

        } catch (Exception e) {

            return null;
        }
    }

    private Pane createHoverPane(
            LineChart<Number, Number> chart,
            NumberAxis xAxis,
            NumberAxis yAxis) {

        Pane hoverPane =
                new Pane();

        hoverPane.setPickOnBounds(true);
        hoverPane.setStyle("-fx-background-color: transparent;");

        Line horizontalLine =
                new Line();

        horizontalLine.setManaged(false);
        horizontalLine.setVisible(false);
        horizontalLine.setStyle(
                "-fx-stroke: #777777;"
                        + "-fx-stroke-width: 1;"
                        + "-fx-stroke-dash-array: 6 4;"
        );

        Line verticalLine =
                new Line();

        verticalLine.setManaged(false);
        verticalLine.setVisible(false);
        verticalLine.setStyle(
                "-fx-stroke: #777777;"
                        + "-fx-stroke-width: 1;"
                        + "-fx-stroke-dash-array: 6 4;"
        );

        Circle hoverPoint =
                new Circle(5);

        hoverPoint.setManaged(false);
        hoverPoint.setVisible(false);
        hoverPoint.setStyle(
                "-fx-fill: white;"
                        + "-fx-stroke: black;"
                        + "-fx-stroke-width: 2;"
        );

        Label hoverBox =
                new Label();

        hoverBox.setManaged(false);
        hoverBox.setVisible(false);
        hoverBox.setStyle(
                "-fx-background-color: white;"
                        + "-fx-border-color: #333333;"
                        + "-fx-border-width: 1;"
                        + "-fx-padding: 6;"
                        + "-fx-font-size: 11px;"
                        + "-fx-text-fill: black;"
        );

        hoverPane.getChildren().addAll(
                horizontalLine,
                verticalLine,
                hoverPoint,
                hoverBox
        );

        hoverPane.setOnMouseMoved(event -> {

            updateHoverOverlay(
                    event.getX(),
                    event.getY(),
                    chart,
                    xAxis,
                    yAxis,
                    hoverPane,
                    horizontalLine,
                    verticalLine,
                    hoverPoint,
                    hoverBox
            );
        });

        hoverPane.setOnMouseExited(event -> {

            horizontalLine.setVisible(false);
            verticalLine.setVisible(false);
            hoverPoint.setVisible(false);
            hoverBox.setVisible(false);
        });

        return hoverPane;
    }

    private void updateHoverOverlay(
            double mouseX,
            double mouseY,
            LineChart<Number, Number> chart,
            NumberAxis xAxis,
            NumberAxis yAxis,
            Pane hoverPane,
            Line horizontalLine,
            Line verticalLine,
            Circle hoverPoint,
            Label hoverBox) {

        Node plotBackground =
                chart.lookup(".chart-plot-background");

        if (plotBackground == null) {
            return;
        }

        Bounds plotBounds =
                hoverPane.sceneToLocal(
                        plotBackground.localToScene(
                                plotBackground.getBoundsInLocal()
                        )
                );

        if (!plotBounds.contains(mouseX, mouseY)) {

            horizontalLine.setVisible(false);
            verticalLine.setVisible(false);
            hoverPoint.setVisible(false);
            hoverBox.setVisible(false);
            return;
        }

        XYChart.Series<Number, Number> closestSeries =
                null;

        XYChart.Data<Number, Number> closestData =
                null;

        double closestX =
                0;

        double closestY =
                0;

        double smallestDistance =
                Double.MAX_VALUE;

        for (XYChart.Series<Number, Number> series
                : chart.getData()) {

            for (XYChart.Data<Number, Number> data
                    : series.getData()) {

                double xValue =
                        data.getXValue().doubleValue();

                double yValue =
                        data.getYValue().doubleValue();

                double displayX =
                        plotBounds.getMinX()
                                + xAxis.getDisplayPosition(xValue);

                double displayY =
                        plotBounds.getMinY()
                                + yAxis.getDisplayPosition(yValue);

                double distance =
                        Math.hypot(
                                mouseX - displayX,
                                mouseY - displayY
                        );

                if (distance < smallestDistance) {

                    smallestDistance = distance;
                    closestSeries = series;
                    closestData = data;
                    closestX = displayX;
                    closestY = displayY;
                }
            }
        }

        if (closestData == null || smallestDistance > 35) {

            horizontalLine.setVisible(false);
            verticalLine.setVisible(false);
            hoverPoint.setVisible(false);
            hoverBox.setVisible(false);
            return;
        }

        StockRecord record =
                (StockRecord) closestData.getExtraValue();

        if (record == null || closestSeries == null) {
            return;
        }

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("MMM dd, yyyy");

        String symbol =
                closestSeries.getName();

        String closeValue =
                String.format(
                        "%.2f",
                        record.getClose()
                );

        horizontalLine.setStartX(plotBounds.getMinX());
        horizontalLine.setEndX(plotBounds.getMaxX());
        horizontalLine.setStartY(closestY);
        horizontalLine.setEndY(closestY);

        verticalLine.setStartX(closestX);
        verticalLine.setEndX(closestX);
        verticalLine.setStartY(plotBounds.getMinY());
        verticalLine.setEndY(plotBounds.getMaxY());

        hoverPoint.setCenterX(closestX);
        hoverPoint.setCenterY(closestY);

        hoverBox.setText(
                symbol
                        + "\nDate: "
                        + record.getDate().format(formatter)
                        + "\nClose: $"
                        + closeValue
        );

        hoverBox.applyCss();
        hoverBox.autosize();

        double boxX =
                closestX + 12;

        double boxY =
                closestY - 45;

        if (boxX + hoverBox.getWidth()
                > plotBounds.getMaxX()) {

            boxX =
                    closestX
                            - hoverBox.getWidth()
                            - 12;
        }

        if (boxY < plotBounds.getMinY()) {
            boxY = plotBounds.getMinY() + 5;
        }

        hoverBox.relocate(
                boxX,
                boxY
        );

        horizontalLine.setVisible(true);
        verticalLine.setVisible(true);
        hoverPoint.setVisible(true);
        hoverBox.setVisible(true);
    }

    private void applyStableSeriesColors(
            LineChart<Number, Number> chart) {

        for (XYChart.Series<Number, Number> series
                : chart.getData()) {

            String color =
                    getColorForSymbol(series.getName());

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

    private String getColorForSymbol(String symbol) {

        return colorBySymbol.computeIfAbsent(
                symbol,
                key -> generateRandomColor()
        );
    }

    private String generateRandomColor() {

        int red =
                80 + random.nextInt(176);

        int green =
                80 + random.nextInt(176);

        int blue =
                80 + random.nextInt(176);

        return String.format(
                "#%02x%02x%02x",
                red,
                green,
                blue
        );
    }

    private static class StockRecord
            implements Comparable<StockRecord> {

        private final String symbol;
        private final LocalDate date;
        private final double close;

        public StockRecord(
                String symbol,
                LocalDate date,
                double close) {

            this.symbol = symbol;
            this.date = date;
            this.close = close;
        }

        public String getSymbol() {
            return symbol;
        }

        public LocalDate getDate() {
            return date;
        }

        public double getClose() {
            return close;
        }

        @Override
        public int compareTo(StockRecord other) {
            return this.date.compareTo(other.date);
        }
    }
}
