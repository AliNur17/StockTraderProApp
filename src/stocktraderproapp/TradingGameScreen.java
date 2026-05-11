package stocktraderproapp;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

public class TradingGameScreen extends BaseScreen {

    private enum GameState { SETUP, PLAYING, SUMMARY }

    private static final double STARTING_CASH = 10_000.0;

    private GameState gameState = GameState.SETUP;
    private Portfolio portfolio;
    private int currentDayIndex;
    private int timeWindow;
    private List<String> selectedSymbols;
    private Map<String, List<StockRecord>> gameRecords;

    private String activeSymbol;
    private final SymbolColorManager colorManager = new SymbolColorManager();

    public TradingGameScreen(ScreenManager manager) {
        super(manager);
    }

    @Override
    public Parent getView() {
        switch (gameState) {
            case PLAYING:  return buildGameView();
            case SUMMARY:  return buildSummaryView();
            default:       return buildSetupView();
        }
    }

    private Parent buildSetupView() {

        Map<String, List<StockRecord>> allRecords = loadAllRecords();
        List<String> watchlistList = new ArrayList<>(WatchlistManager.getSymbols());

        Label title = new Label("Stock Trading Game");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold;");

        Label subtitle = new Label("Select stocks from your watchlist to trade.");
        subtitle.setStyle("-fx-text-fill: #666666;");

        VBox checkBoxContainer = new VBox(8);
        List<CheckBox> checkBoxList = new ArrayList<>();

        if (watchlistList.isEmpty()) {
            Label empty = new Label(
                    "Your watchlist is empty. Add stocks from the Search screen first."
            );
            empty.setStyle("-fx-text-fill: #888888;");
            empty.setWrapText(true);
            checkBoxContainer.getChildren().add(empty);
        } else {
            for (String symbol : watchlistList) {

                boolean hasData = allRecords.containsKey(symbol)
                        && !allRecords.get(symbol).isEmpty();

                String company = StockInfo.COMPANY_NAMES.getOrDefault(symbol, "");
                String labelText = company.isEmpty()
                        ? symbol
                        : symbol + "  —  " + company;

                if (!hasData) {
                    labelText += "  (no data)";
                }

                CheckBox cb = new CheckBox(labelText);
                cb.setSelected(hasData);
                cb.setDisable(!hasData);

                checkBoxList.add(cb);
                checkBoxContainer.getChildren().add(cb);
            }
        }

        ComboBox<String> windowBox = new ComboBox<>();
        windowBox.getItems().addAll("30 Days", "50 Days", "70 Days");
        windowBox.setValue("50 Days");

        HBox windowRow = new HBox(10, new Label("Time window:"), windowBox);
        windowRow.setAlignment(Pos.CENTER_LEFT);

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #c62828;");

        Button startButton = new Button("Start Game");
        startButton.setMaxWidth(Double.MAX_VALUE);

        Button backButton = new Button("Back");
        backButton.setMaxWidth(Double.MAX_VALUE);
        backButton.setOnAction(e -> manager.show(Main.HOME));

        startButton.setOnAction(e -> {

            List<String> chosen = new ArrayList<>();

            for (int i = 0; i < watchlistList.size(); i++) {
                if (i < checkBoxList.size()
                        && checkBoxList.get(i).isSelected()) {
                    chosen.add(watchlistList.get(i));
                }
            }

            if (chosen.isEmpty()) {
                errorLabel.setText("Select at least one stock to start.");
                return;
            }

            int window = windowBox.getValue().startsWith("30") ? 30
                    : windowBox.getValue().startsWith("70") ? 70 : 50;

            initGame(chosen, allRecords, window);
            manager.show(Main.TRADING_GAME);
        });

        VBox content = new VBox(
                16,
                title,
                subtitle,
                checkBoxContainer,
                new Separator(),
                windowRow,
                new Label("Starting cash: $10,000"),
                errorLabel,
                startButton,
                backButton
        );

        content.setPadding(new Insets(30));
        content.setMaxWidth(540);

        BorderPane root = new BorderPane(content);
        BorderPane.setAlignment(content, Pos.CENTER);

        return root;
    }

    private void initGame(
            List<String> chosen,
            Map<String, List<StockRecord>> allRecords,
            int window) {

        TreeSet<LocalDate> commonDates = null;

        for (String symbol : chosen) {

            List<StockRecord> records = allRecords.get(symbol);
            TreeSet<LocalDate> dates = new TreeSet<>();

            for (StockRecord record : records) {
                dates.add(record.getDate());
            }

            if (commonDates == null) {
                commonDates = dates;
            } else {
                commonDates.retainAll(dates);
            }
        }

        if (commonDates == null || commonDates.isEmpty()) {
            return;
        }

        List<LocalDate> gameDates = new ArrayList<>(commonDates);

        if (gameDates.size() > window) {
            gameDates = gameDates.subList(
                    gameDates.size() - window,
                    gameDates.size()
            );
        }

        gameRecords = new HashMap<>();

        for (String symbol : chosen) {

            Map<LocalDate, StockRecord> byDate = new HashMap<>();

            for (StockRecord record : allRecords.get(symbol)) {
                byDate.put(record.getDate(), record);
            }

            List<StockRecord> gameList = new ArrayList<>();

            for (LocalDate date : gameDates) {
                if (byDate.containsKey(date)) {
                    gameList.add(byDate.get(date));
                }
            }

            gameRecords.put(symbol, gameList);
        }

        selectedSymbols = chosen;
        activeSymbol = chosen.get(0);
        timeWindow = gameDates.size();
        currentDayIndex = 0;
        portfolio = new Portfolio(STARTING_CASH);
        gameState = GameState.PLAYING;
    }

    private Parent buildGameView() {

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");

        Map<String, StockRecord> todayRecords = getTodayRecords();
        Map<String, Double> currentPrices = getCurrentPrices(todayRecords);

        StockRecord activeRecord = todayRecords.get(activeSymbol);

        String dateText = activeRecord == null
                ? ""
                : activeRecord.getDate().format(dateFormatter);

        Label dateLabel = new Label("[ " + dateText + " ]");
        dateLabel.setStyle("-fx-font-size: 15px; -fx-text-fill: #555555;");

        Label totalBalanceLabel = new Label();
        totalBalanceLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold;");

        Label currentBalanceLabel = new Label();
        currentBalanceLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #666666;");

        VBox stockButtonBox = new VBox(8);
        stockButtonBox.setPadding(new Insets(8));

        ScrollPane stockScroll = new ScrollPane(stockButtonBox);
        stockScroll.setFitToWidth(true);
        stockScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        stockScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(stockScroll, Priority.ALWAYS);

        Label stockTitle = new Label("Stocks:");
        stockTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Button quitButton = new Button("Quit Game");
        quitButton.setMaxWidth(Double.MAX_VALUE);
        quitButton.setOnAction(e -> {
            gameState = GameState.SETUP;
            manager.show(Main.TRADING_GAME);
        });

        VBox leftBar = new VBox(
                10,
                dateLabel,
                totalBalanceLabel,
                currentBalanceLabel,
                new Separator(),
                stockTitle,
                stockScroll,
                quitButton
        );

        leftBar.setPadding(new Insets(18));
        leftBar.setPrefWidth(260);
        leftBar.setMinWidth(260);
        leftBar.setMaxWidth(260);
        leftBar.setStyle(
                "-fx-background-color: #fafafa;"
                        + "-fx-border-color: #dddddd;"
                        + "-fx-border-width: 0 1 0 0;"
        );

        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();

        xAxis.setLabel("Date");
        yAxis.setLabel("Closing Price ($)");
        xAxis.setAutoRanging(false);
        yAxis.setAutoRanging(false);

        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setCreateSymbols(false);
        chart.setAnimated(false);
        chart.setLegendVisible(false);
        chart.setPadding(new Insets(20, 40, 10, 40));

        Pane hoverPane = new ChartHoverOverlay(chart, xAxis, yAxis).getPane();

        StackPane chartStack = new StackPane(chart, hoverPane);
        VBox.setVgrow(chartStack, Priority.ALWAYS);

        Label selectedStockLabel = new Label();
        selectedStockLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");

        Label priceLabel = new Label();
        priceLabel.setStyle("-fx-font-size: 14px;");

        Label holdingsLabel = new Label();
        holdingsLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #555555;");

        Spinner<Integer> shareSpinner = new Spinner<>();
        shareSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10_000, 1)
        );
        shareSpinner.setEditable(true);
        shareSpinner.setPrefWidth(110);

        Button buyButton = new Button("Buy");
        Button sellButton = new Button("Sell");

        buyButton.setPrefWidth(80);
        sellButton.setPrefWidth(80);

        Button nextButton = new Button();
        nextButton.setPrefWidth(160);

        HBox tradeControls = new HBox(
                8,
                buyButton,
                sellButton,
                new Label("# of shares"),
                shareSpinner,
                nextButton
        );
        tradeControls.setAlignment(Pos.CENTER_LEFT);

        VBox detailBox = new VBox(
                8,
                selectedStockLabel,
                priceLabel,
                holdingsLabel,
                tradeControls
        );

        detailBox.setPadding(new Insets(10, 30, 20, 30));

        VBox centerArea = new VBox(chartStack, detailBox);
        VBox.setVgrow(chartStack, Priority.ALWAYS);

        Runnable refreshAll = () -> {

            Map<String, StockRecord> refreshedToday = getTodayRecords();
            Map<String, Double> refreshedPrices = getCurrentPrices(refreshedToday);

            updateBalanceLabels(
                    totalBalanceLabel,
                    currentBalanceLabel,
                    refreshedPrices
            );

            refreshStockButtons(stockButtonBox);
            updateGameChart(chart, xAxis, yAxis);
            updateSelectedStockDetails(
                    selectedStockLabel,
                    priceLabel,
                    holdingsLabel,
                    buyButton,
                    sellButton,
                    shareSpinner
            );

            boolean isLastDay = currentDayIndex >= timeWindow - 1;
            nextButton.setText(isLastDay ? "Finish Game" : "Next Day");
        };

        buyButton.setOnAction(e -> {

            StockRecord record = getActiveRecord();

            if (record == null) {
                return;
            }

            int shares = shareSpinner.getValue();
            double price = record.getClose();

            for (int i = 0; i < shares; i++) {
                if (portfolio.getCash() >= price) {
                    portfolio.buy(activeSymbol, price);
                }
            }

            refreshAll.run();
        });

        sellButton.setOnAction(e -> {

            StockRecord record = getActiveRecord();

            if (record == null) {
                return;
            }

            int shares = shareSpinner.getValue();
            double price = record.getClose();

            for (int i = 0; i < shares; i++) {
                if (portfolio.getShares(activeSymbol) > 0) {
                    portfolio.sell(activeSymbol, price);
                }
            }

            refreshAll.run();
        });

        nextButton.setOnAction(e -> {

            if (currentDayIndex >= timeWindow - 1) {
                gameState = GameState.SUMMARY;
            } else {
                currentDayIndex++;
            }

            manager.show(Main.TRADING_GAME);
        });

        refreshAll.run();

        BorderPane root = new BorderPane();
        root.setLeft(leftBar);
        root.setCenter(centerArea);

        return root;
    }

    private void refreshStockButtons(VBox stockButtonBox) {

        stockButtonBox.getChildren().clear();

        Map<String, StockRecord> todayRecords = getTodayRecords();

        for (String symbol : selectedSymbols) {

            StockRecord record = todayRecords.get(symbol);

            if (record == null) {
                continue;
            }

            String company = StockInfo.COMPANY_NAMES.getOrDefault(symbol, "");
            String labelText = company.isEmpty()
                    ? symbol + "\n$" + String.format("%.2f", record.getClose())
                    : symbol + "  $" + String.format("%.2f", record.getClose())
                    + "\n" + company;

            Button button = new Button(labelText);
            button.setMaxWidth(Double.MAX_VALUE);
            button.setAlignment(Pos.CENTER_LEFT);

            if (symbol.equals(activeSymbol)) {
                button.setStyle(
                        "-fx-font-weight: bold;"
                                + "-fx-border-width: 2;"
                                + "-fx-border-color: "
                                + colorManager.getColorForSymbol(symbol)
                                + ";"
                );
            }

            button.setOnAction(e -> {
                activeSymbol = symbol;
                manager.show(Main.TRADING_GAME);
            });

            stockButtonBox.getChildren().add(button);
        }
    }

    private void updateBalanceLabels(
            Label totalBalanceLabel,
            Label currentBalanceLabel,
            Map<String, Double> currentPrices) {

        double totalValue = portfolio.getTotalValue(currentPrices);
        double pnl = totalValue - STARTING_CASH;

        totalBalanceLabel.setText(String.format("$%.2f Total Balance", totalValue));

        currentBalanceLabel.setText(String.format(
                "Current Balance: $%.2f     P&L: %s$%.2f",
                portfolio.getCash(),
                pnl >= 0 ? "+" : "",
                pnl
        ));

        totalBalanceLabel.setStyle(
                "-fx-font-size: 28px;"
                        + "-fx-font-weight: bold;"
                        + "-fx-text-fill: "
                        + (pnl >= 0 ? "#2e7d32" : "#c62828")
                        + ";"
        );
    }

    private void updateSelectedStockDetails(
            Label selectedStockLabel,
            Label priceLabel,
            Label holdingsLabel,
            Button buyButton,
            Button sellButton,
            Spinner<Integer> shareSpinner) {

        StockRecord record = getActiveRecord();

        if (record == null) {
            selectedStockLabel.setText("No stock selected");
            priceLabel.setText("");
            holdingsLabel.setText("");
            buyButton.setDisable(true);
            sellButton.setDisable(true);
            return;
        }

        double price = record.getClose();
        int shares = portfolio.getShares(activeSymbol);
        double holdingValue = shares * price;

        String company = StockInfo.COMPANY_NAMES.getOrDefault(activeSymbol, "");

        selectedStockLabel.setText(
                company.isEmpty()
                        ? "Stock: " + activeSymbol
                        : "Stock: " + activeSymbol + " — " + company
        );

        priceLabel.setText(String.format("Current Price: $%.2f per share", price));

        holdingsLabel.setText(String.format(
                "Current Holdings: %d shares     $%.2f",
                shares,
                holdingValue
        ));

        int requestedShares = shareSpinner.getValue();

        buyButton.setDisable(portfolio.getCash() < price || requestedShares <= 0);
        sellButton.setDisable(shares <= 0 || requestedShares <= 0);
    }

    private void updateGameChart(
            LineChart<Number, Number> chart,
            NumberAxis xAxis,
            NumberAxis yAxis) {

        chart.getData().clear();

        if (activeSymbol == null || !gameRecords.containsKey(activeSymbol)) {
            chart.setTitle("Select a Stock");
            return;
        }

        List<StockRecord> records = gameRecords.get(activeSymbol);

        if (records == null || records.isEmpty()) {
            chart.setTitle("No data available");
            return;
        }

        XYChart.Series<Number, Number> visibleSeries = new XYChart.Series<>();
        visibleSeries.setName(activeSymbol);

        double minClose = Double.MAX_VALUE;
        double maxClose = 0;

        for (int i = 0; i <= currentDayIndex && i < records.size(); i++) {

            StockRecord record = records.get(i);

            XYChart.Data<Number, Number> point =
                    new XYChart.Data<>(i, record.getClose());

            point.setExtraValue(record);
            visibleSeries.getData().add(point);

            minClose = Math.min(minClose, record.getClose());
            maxClose = Math.max(maxClose, record.getClose());
        }

        chart.getData().add(visibleSeries);

        configureXAxis(xAxis, records);
        configureYAxis(yAxis, minClose, maxClose);

        StockRecord current = records.get(currentDayIndex);

        chart.setTitle(
                activeSymbol
                        + " Trading View — Current Date: "
                        + current.getDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
        );

        Platform.runLater(() -> applySeriesColor(chart, activeSymbol));
    }

    private void configureXAxis(
            NumberAxis xAxis,
            List<StockRecord> records) {

        int finalDayIndex = records.size();

        xAxis.setLowerBound(0);
        xAxis.setUpperBound(Math.max(0, finalDayIndex - 1));
        xAxis.setTickUnit(Math.max(1, finalDayIndex / 8));

        xAxis.setTickLabelFormatter(new StringConverter<Number>() {

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd");

            @Override
            public String toString(Number value) {

                int index = value.intValue();

                if (index >= 0 && index < records.size()) {
                    return records.get(index).getDate().format(formatter);
                }

                return "";
            }

            @Override
            public Number fromString(String string) {
                return 0;
            }
        });
    }

    private void configureYAxis(
            NumberAxis yAxis,
            double minClose,
            double maxClose) {

        if (minClose == Double.MAX_VALUE) {
            minClose = 0;
            maxClose = 1;
        }

        double padding = Math.max(1, (maxClose - minClose) * 0.20);

        yAxis.setLowerBound(Math.max(0, minClose - padding));
        yAxis.setUpperBound(maxClose + padding);
        yAxis.setTickUnit(Math.max(1, (maxClose - minClose) / 8));
    }

    private void applySeriesColor(
            LineChart<Number, Number> chart,
            String symbol) {

        String color = colorManager.getColorForSymbol(symbol);

        for (XYChart.Series<Number, Number> series : chart.getData()) {

            Node seriesNode = series.getNode();

            if (seriesNode != null) {

                Node line = seriesNode.lookup(".chart-series-line");

                if (line != null) {
                    line.setStyle(
                            "-fx-stroke: "
                                    + color
                                    + ";"
                                    + "-fx-stroke-width: 2.5px;"
                    );
                }
            }
        }
    }

    private Map<String, StockRecord> getTodayRecords() {

        Map<String, StockRecord> todayRecords = new HashMap<>();

        if (selectedSymbols == null || gameRecords == null) {
            return todayRecords;
        }

        for (String symbol : selectedSymbols) {

            List<StockRecord> records = gameRecords.get(symbol);

            if (records != null && currentDayIndex < records.size()) {
                todayRecords.put(symbol, records.get(currentDayIndex));
            }
        }

        return todayRecords;
    }

    private Map<String, Double> getCurrentPrices(
            Map<String, StockRecord> todayRecords) {

        Map<String, Double> currentPrices = new HashMap<>();

        for (Map.Entry<String, StockRecord> entry : todayRecords.entrySet()) {
            currentPrices.put(entry.getKey(), entry.getValue().getClose());
        }

        return currentPrices;
    }

    private StockRecord getActiveRecord() {

        if (activeSymbol == null || gameRecords == null) {
            return null;
        }

        List<StockRecord> records = gameRecords.get(activeSymbol);

        if (records == null || currentDayIndex >= records.size()) {
            return null;
        }

        return records.get(currentDayIndex);
    }

    private Parent buildSummaryView() {

        Map<String, Double> lastPrices = new HashMap<>();
        Map<String, Double> firstPrices = new HashMap<>();

        for (String symbol : selectedSymbols) {

            List<StockRecord> records = gameRecords.get(symbol);

            if (records != null && !records.isEmpty()) {
                firstPrices.put(symbol, records.get(0).getClose());
                lastPrices.put(symbol, records.get(records.size() - 1).getClose());
            }
        }

        double finalValue = portfolio.getTotalValue(lastPrices);
        double pnl = finalValue - STARTING_CASH;
        double pnlPct = (pnl / STARTING_CASH) * 100;

        double cashPerStock = STARTING_CASH / selectedSymbols.size();
        double buyHoldValue = 0;

        for (String symbol : selectedSymbols) {

            double firstPrice = firstPrices.getOrDefault(symbol, 0.0);
            double lastPrice = lastPrices.getOrDefault(symbol, 0.0);

            if (firstPrice > 0) {
                int shares = (int) (cashPerStock / firstPrice);
                double remainder = cashPerStock - (shares * firstPrice);
                buyHoldValue += (shares * lastPrice) + remainder;
            }
        }

        double bhPnl = buyHoldValue - STARTING_CASH;

        Label title = new Label("Game Over!");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold;");

        Label finalLabel = new Label(
                String.format("Final Portfolio Value: $%.2f", finalValue)
        );

        Label pnlLabel = new Label(String.format(
                "Your P&L: %s$%.2f  (%s%.1f%%)",
                pnl >= 0 ? "+" : "",
                pnl,
                pnl >= 0 ? "+" : "",
                pnlPct
        ));

        pnlLabel.setStyle(
                "-fx-font-size: 15px;"
                        + "-fx-font-weight: bold;"
                        + "-fx-text-fill: "
                        + (pnl >= 0 ? "#2e7d32" : "#c62828")
                        + ";"
        );

        Label buyHoldLabel = new Label(String.format(
                "Buy & Hold P&L: %s$%.2f",
                bhPnl >= 0 ? "+" : "",
                bhPnl
        ));

        String vsText;

        if (pnl > bhPnl) {
            vsText = "You beat buy & hold!";
        } else if (pnl < bhPnl) {
            vsText = "Buy & hold beat you this time.";
        } else {
            vsText = "You matched buy & hold exactly!";
        }

        Label vsLabel = new Label(vsText);
        vsLabel.setStyle("-fx-font-style: italic;");

        Button playAgainButton = new Button("Play Again");
        playAgainButton.setOnAction(e -> {
            gameState = GameState.SETUP;
            manager.show(Main.TRADING_GAME);
        });

        Button homeButton = new Button("Back to Home");
        homeButton.setOnAction(e -> {
            gameState = GameState.SETUP;
            manager.show(Main.HOME);
        });

        HBox buttons = new HBox(12, playAgainButton, homeButton);
        buttons.setAlignment(Pos.CENTER);

        VBox content = new VBox(
                16,
                title,
                new Separator(),
                finalLabel,
                pnlLabel,
                buyHoldLabel,
                vsLabel,
                new Separator(),
                buttons
        );

        content.setPadding(new Insets(40));
        content.setMaxWidth(500);

        BorderPane root = new BorderPane(content);
        BorderPane.setAlignment(content, Pos.CENTER);

        return root;
    }

    private Map<String, List<StockRecord>> loadAllRecords() {

        try {
            return new StockDataFileManager(StockInfo.STOCK_DATA_FILE)
                    .loadAllRecords();
        } catch (IOException e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }
}