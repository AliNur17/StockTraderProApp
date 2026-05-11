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
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

public class TradingGameScreen extends BaseScreen {

    private enum GameState { SETUP, PLAYING, SUMMARY }

    private static final double STARTING_CASH = 10_000.0;
    private static final String SP_SYMBOL = "SPY";

    private GameState gameState = GameState.SETUP;

    private Portfolio portfolio;

    private int currentDayIndex;
    private int timeWindow;

    private List<String> selectedSymbols;
    private Map<String, List<StockRecord>> gameRecords;

    private String activeSymbol;

    private List<Double> playerValueHistory;

    private final GraphInterface graphInterface = new StockGraph();

    private final SymbolColorManager colorManager = new SymbolColorManager();

    public TradingGameScreen(ScreenManager manager) {
        super(manager);
    }

    @Override
    public Parent getView() {

        switch (gameState) {

            case PLAYING:
                return buildGameView();

            case SUMMARY:
                return buildSummaryView();

            default:
                return buildSetupView();
        }
    }

    private Parent buildSetupView() {

        Map<String, List<StockRecord>> allRecords = loadAllRecords();

        List<String> watchlistList =
                new ArrayList<>(WatchlistManager.getSymbols());

        Label title = new Label("Stock Trading Game");

        title.setStyle(
                "-fx-font-size: 26px;"
                        + "-fx-font-weight: bold;"
        );

        Label subtitle =
                new Label("Select stocks from your watchlist to trade.");

        subtitle.setStyle("-fx-text-fill: #666666;");

        VBox checkBoxContainer = new VBox(8);

        List<CheckBox> checkBoxList = new ArrayList<>();

        if (watchlistList.isEmpty()) {

            Label empty =
                    new Label(
                            "Your watchlist is empty. Add stocks from the Search screen first."
                    );

            empty.setStyle("-fx-text-fill: #888888;");
            empty.setWrapText(true);

            checkBoxContainer.getChildren().add(empty);

        } else {

            for (String symbol : watchlistList) {

                boolean hasData =
                        hasUsableData(allRecords, symbol);

                String company =
                        StockInfo.COMPANY_NAMES.getOrDefault(symbol, "");

                String labelText =
                        company.isEmpty()
                                ? symbol
                                : symbol + "  —  " + company;

                if (!hasData) {
                    labelText += "  (no data)";
                }

                CheckBox checkBox = new CheckBox(labelText);

                checkBox.setSelected(hasData);
                checkBox.setDisable(!hasData);

                checkBoxList.add(checkBox);
                checkBoxContainer.getChildren().add(checkBox);
            }
        }

        ComboBox<String> windowBox = new ComboBox<>();

        windowBox.getItems().addAll(
                "30 Days",
                "50 Days",
                "70 Days"
        );

        windowBox.setValue("50 Days");

        HBox windowRow =
                new HBox(
                        10,
                        new Label("Time window:"),
                        windowBox
                );

        windowRow.setAlignment(Pos.CENTER_LEFT);

        Label errorLabel = new Label();

        errorLabel.setStyle("-fx-text-fill: #c62828;");

        Button startButton = new Button("Start Game");

        startButton.setMaxWidth(Double.MAX_VALUE);

        Button backButton = new Button("Back");

        backButton.setMaxWidth(Double.MAX_VALUE);

        backButton.setOnAction(
                e -> manager.show(Main.HOME)
        );

        startButton.setOnAction(
                e -> startGameFromSetup(
                        watchlistList,
                        checkBoxList,
                        allRecords,
                        windowBox,
                        errorLabel
                )
        );

        VBox content =
                new VBox(
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

    private void startGameFromSetup(
            List<String> watchlistList,
            List<CheckBox> checkBoxList,
            Map<String, List<StockRecord>> allRecords,
            ComboBox<String> windowBox,
            Label errorLabel) {

        List<String> chosen =
                getChosenSymbols(watchlistList, checkBoxList);

        if (chosen.isEmpty()) {
            errorLabel.setText("Select at least one stock to start.");
            return;
        }

        int window =
                parseWindowSelection(windowBox.getValue());

        initGame(chosen, allRecords, window);

        manager.show(Main.TRADING_GAME);
    }

    private List<String> getChosenSymbols(
            List<String> watchlistList,
            List<CheckBox> checkBoxList) {

        List<String> chosen = new ArrayList<>();

        for (int i = 0; i < watchlistList.size(); i++) {

            if (i < checkBoxList.size()
                    && checkBoxList.get(i).isSelected()) {

                chosen.add(watchlistList.get(i));
            }
        }

        return chosen;
    }

    private int parseWindowSelection(String value) {

        if (value.startsWith("30")) {
            return 30;
        }

        if (value.startsWith("70")) {
            return 70;
        }

        return 50;
    }

    private boolean hasUsableData(
            Map<String, List<StockRecord>> allRecords,
            String symbol) {

        return allRecords.containsKey(symbol)
                && allRecords.get(symbol) != null
                && !allRecords.get(symbol).isEmpty();
    }

    private void initGame(
            List<String> chosen,
            Map<String, List<StockRecord>> allRecords,
            int window) {

        List<LocalDate> gameDates =
                getCommonGameDates(chosen, allRecords, window);

        if (gameDates.isEmpty()) {
            return;
        }

        gameRecords =
                buildGameRecords(chosen, allRecords, gameDates);

        selectedSymbols = chosen;
        activeSymbol = chosen.get(0);
        timeWindow = gameDates.size();
        currentDayIndex = 0;
        portfolio = new Portfolio(STARTING_CASH);

        playerValueHistory = new ArrayList<>();
        playerValueHistory.add(STARTING_CASH);

        gameState = GameState.PLAYING;
    }

    private List<LocalDate> getCommonGameDates(
            List<String> chosen,
            Map<String, List<StockRecord>> allRecords,
            int window) {

        TreeSet<LocalDate> commonDates = null;

        for (String symbol : chosen) {

            TreeSet<LocalDate> dates = new TreeSet<>();

            for (StockRecord record : allRecords.get(symbol)) {
                dates.add(record.getDate());
            }

            if (commonDates == null) {
                commonDates = dates;
            } else {
                commonDates.retainAll(dates);
            }
        }

        if (commonDates == null) {
            return new ArrayList<>();
        }

        List<LocalDate> gameDates =
                new ArrayList<>(commonDates);

        if (gameDates.size() > window) {

            gameDates =
                    gameDates.subList(
                            gameDates.size() - window,
                            gameDates.size()
                    );
        }

        return gameDates;
    }

    private Map<String, List<StockRecord>> buildGameRecords(
            List<String> chosen,
            Map<String, List<StockRecord>> allRecords,
            List<LocalDate> gameDates) {

        Map<String, List<StockRecord>> recordsBySymbol =
                new HashMap<>();

        for (String symbol : chosen) {

            Map<LocalDate, StockRecord> byDate =
                    new HashMap<>();

            for (StockRecord record : allRecords.get(symbol)) {
                byDate.put(record.getDate(), record);
            }

            List<StockRecord> gameList = new ArrayList<>();

            for (LocalDate date : gameDates) {

                if (byDate.containsKey(date)) {
                    gameList.add(byDate.get(date));
                }
            }

            recordsBySymbol.put(symbol, gameList);
        }

        return recordsBySymbol;
    }

    private Parent buildGameView() {

        VBox stockButtonBox = createStockButtonBox();

        VBox leftBar = createLeftBar(stockButtonBox);

        VBox detailBox = createDetailBox();

        VBox centerArea =
                new VBox(
                        graphInterface.getView(),
                        detailBox
                );

        VBox.setVgrow(
                graphInterface.getView(),
                Priority.ALWAYS
        );

        refreshGameView(stockButtonBox);

        BorderPane root = new BorderPane();

        root.setLeft(leftBar);
        root.setCenter(centerArea);

        return root;
    }

    private VBox createStockButtonBox() {

        VBox stockButtonBox = new VBox(8);

        stockButtonBox.setPadding(new Insets(8));

        return stockButtonBox;
    }

    private VBox createLeftBar(VBox stockButtonBox) {

        Label dateLabel = new Label(getCurrentDateText());

        dateLabel.setStyle(
                "-fx-font-size: 15px;"
                        + "-fx-text-fill: #555555;"
        );

        Label totalBalanceLabel =
                new Label(getTotalBalanceText());

        totalBalanceLabel.setStyle(getTotalBalanceStyle());

        Label currentBalanceLabel =
                new Label(getCurrentBalanceText());

        currentBalanceLabel.setStyle(
                "-fx-font-size: 13px;"
                        + "-fx-text-fill: #666666;"
        );

        Label stockTitle = new Label("Stocks:");

        stockTitle.setStyle(
                "-fx-font-size: 16px;"
                        + "-fx-font-weight: bold;"
        );

        ScrollPane stockScroll =
                new ScrollPane(stockButtonBox);

        stockScroll.setFitToWidth(true);
        stockScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        stockScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        VBox.setVgrow(stockScroll, Priority.ALWAYS);

        Button quitButton = new Button("Quit Game");

        quitButton.setMaxWidth(Double.MAX_VALUE);

        quitButton.setOnAction(e -> {
            gameState = GameState.SETUP;
            manager.show(Main.TRADING_GAME);
        });

        VBox leftBar =
                new VBox(
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

        return leftBar;
    }

    private VBox createDetailBox() {

        Label selectedStockLabel =
                new Label(getSelectedStockText());

        selectedStockLabel.setStyle(
                "-fx-font-size: 22px;"
                        + "-fx-font-weight: bold;"
        );

        Label priceLabel = new Label(getPriceText());

        priceLabel.setStyle("-fx-font-size: 14px;");

        Label holdingsLabel = new Label(getHoldingsText());

        holdingsLabel.setStyle(
                "-fx-font-size: 14px;"
                        + "-fx-text-fill: #555555;"
        );

        Spinner<Integer> shareSpinner = new Spinner<>();

        shareSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(
                        1,
                        10_000,
                        1
                )
        );

        shareSpinner.setEditable(true);
        shareSpinner.setPrefWidth(110);

        Button buyButton = new Button("Buy");
        Button sellButton = new Button("Sell");
        Button nextButton = new Button(getNextButtonText());

        buyButton.setPrefWidth(80);
        sellButton.setPrefWidth(80);
        nextButton.setPrefWidth(160);

        buyButton.setDisable(!canBuy(shareSpinner.getValue()));
        sellButton.setDisable(!canSell(shareSpinner.getValue()));

        buyButton.setOnAction(e -> {
            buyShares(shareSpinner.getValue());
            manager.show(Main.TRADING_GAME);
        });

        sellButton.setOnAction(e -> {
            sellShares(shareSpinner.getValue());
            manager.show(Main.TRADING_GAME);
        });

        nextButton.setOnAction(
                e -> goToNextDayOrSummary()
        );

        HBox tradeControls =
                new HBox(
                        8,
                        buyButton,
                        sellButton,
                        new Label("# of shares"),
                        shareSpinner,
                        nextButton
                );

        tradeControls.setAlignment(Pos.CENTER_LEFT);

        VBox detailBox =
                new VBox(
                        8,
                        selectedStockLabel,
                        priceLabel,
                        holdingsLabel,
                        tradeControls
                );

        detailBox.setPadding(
                new Insets(10, 30, 20, 30)
        );

        return detailBox;
    }

    private void refreshGameView(VBox stockButtonBox) {

        refreshStockButtons(stockButtonBox);

        graphInterface.showRecordsThroughDay(
                getGraphTitle(),
                activeSymbol,
                getActiveRecords(),
                currentDayIndex
        );
    }

    private String getGraphTitle() {

        StockRecord record = getActiveRecord();

        if (record == null) {
            return "Select a Stock";
        }

        return activeSymbol
                + " Trading View — Current Date: "
                + record.getDate().format(
                        DateTimeFormatter.ofPattern("MMM dd, yyyy")
                );
    }

    private void refreshStockButtons(VBox stockButtonBox) {

        stockButtonBox.getChildren().clear();

        for (String symbol : selectedSymbols) {

            StockRecord record = getRecordForSymbol(symbol);

            if (record == null) {
                continue;
            }

            Button button = createStockButton(symbol, record);

            stockButtonBox.getChildren().add(button);
        }
    }

    private Button createStockButton(
            String symbol,
            StockRecord record) {

        String company =
                StockInfo.COMPANY_NAMES.getOrDefault(symbol, "");

        String labelText =
                company.isEmpty()
                        ? symbol + "\n$" + String.format("%.2f", record.getClose())
                        : symbol
                        + "  $"
                        + String.format("%.2f", record.getClose())
                        + "\n"
                        + company;

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

        return button;
    }

    private String getCurrentDateText() {

        StockRecord record = getActiveRecord();

        if (record == null) {
            return "[ No Date ]";
        }

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("MMM dd, yyyy");

        return "[ "
                + record.getDate().format(formatter)
                + " ]";
    }

    private String getTotalBalanceText() {

        return String.format(
                "$%.2f Total Balance",
                getTotalValue()
        );
    }

    private String getCurrentBalanceText() {

        double pnl = getTotalValue() - STARTING_CASH;

        return String.format(
                "Current Balance: $%.2f     P&L: %s$%.2f",
                portfolio.getCash(),
                pnl >= 0 ? "+" : "",
                pnl
        );
    }

    private String getTotalBalanceStyle() {

        double pnl = getTotalValue() - STARTING_CASH;

        return "-fx-font-size: 28px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-fill: "
                + (pnl >= 0 ? "#2e7d32" : "#c62828")
                + ";";
    }

    private String getSelectedStockText() {

        String company =
                StockInfo.COMPANY_NAMES.getOrDefault(activeSymbol, "");

        if (company.isEmpty()) {
            return "Stock: " + activeSymbol;
        }

        return "Stock: "
                + activeSymbol
                + " — "
                + company;
    }

    private String getPriceText() {

        StockRecord record = getActiveRecord();

        if (record == null) {
            return "Current Price: N/A";
        }

        return String.format(
                "Current Price: $%.2f per share",
                record.getClose()
        );
    }

    private String getHoldingsText() {

        StockRecord record = getActiveRecord();

        if (record == null) {
            return "Current Holdings: 0 shares     $0.00";
        }

        int shares = portfolio.getShares(activeSymbol);

        double value = shares * record.getClose();

        return String.format(
                "Current Holdings: %d shares     $%.2f",
                shares,
                value
        );
    }

    private String getNextButtonText() {

        if (isLastDay()) {
            return "Finish Game";
        }

        return "Next Day";
    }

    private boolean canBuy(int shares) {

        StockRecord record = getActiveRecord();

        if (record == null || shares <= 0) {
            return false;
        }

        return portfolio.getCash()
                >= record.getClose();
    }

    private boolean canSell(int shares) {

        if (shares <= 0) {
            return false;
        }

        return portfolio.getShares(activeSymbol) > 0;
    }

    private void buyShares(int shares) {

        StockRecord record = getActiveRecord();

        if (record == null) {
            return;
        }

        double price = record.getClose();

        for (int i = 0; i < shares; i++) {

            if (portfolio.getCash() >= price) {
                portfolio.buy(activeSymbol, price);
            }
        }
    }

    private void sellShares(int shares) {

        StockRecord record = getActiveRecord();

        if (record == null) {
            return;
        }

        double price = record.getClose();

        for (int i = 0; i < shares; i++) {

            if (portfolio.getShares(activeSymbol) > 0) {
                portfolio.sell(activeSymbol, price);
            }
        }
    }

    private void goToNextDayOrSummary() {

        playerValueHistory.add(getTotalValue());

        if (isLastDay()) {
            gameState = GameState.SUMMARY;
        } else {
            currentDayIndex++;
        }

        manager.show(Main.TRADING_GAME);
    }

    private boolean isLastDay() {
        return currentDayIndex >= timeWindow - 1;
    }

    private double getTotalValue() {

        return portfolio.getTotalValue(getCurrentPrices());
    }

    private Map<String, Double> getCurrentPrices() {

        Map<String, Double> prices = new HashMap<>();

        for (String symbol : selectedSymbols) {

            StockRecord record = getRecordForSymbol(symbol);

            if (record != null) {
                prices.put(symbol, record.getClose());
            }
        }

        return prices;
    }

    private StockRecord getRecordForSymbol(String symbol) {

        List<StockRecord> records = gameRecords.get(symbol);

        if (records == null
                || currentDayIndex >= records.size()) {

            return null;
        }

        return records.get(currentDayIndex);
    }

    private StockRecord getActiveRecord() {

        return getRecordForSymbol(activeSymbol);
    }

    private List<StockRecord> getActiveRecords() {

        if (activeSymbol == null || gameRecords == null) {
            return new ArrayList<>();
        }

        List<StockRecord> records = gameRecords.get(activeSymbol);

        if (records == null) {
            return new ArrayList<>();
        }

        return records;
    }

    private Parent buildSummaryView() {

        Map<String, Double> lastPrices = new HashMap<>();

        for (String symbol : selectedSymbols) {

            List<StockRecord> records = gameRecords.get(symbol);

            if (records != null && !records.isEmpty()) {

                lastPrices.put(
                        symbol,
                        records.get(records.size() - 1).getClose()
                );
            }
        }

        double finalValue =
                portfolio.getTotalValue(lastPrices);

        double pnl = finalValue - STARTING_CASH;

        double pnlPct =
                (pnl / STARTING_CASH) * 100;

        List<Double> spIncrementalValues =
                calculateIncrementalSPValues();

        double spFinalValue =
                spIncrementalValues.isEmpty()
                        ? STARTING_CASH
                        : spIncrementalValues.get(
                                spIncrementalValues.size() - 1
                        );

        double spPnl = spFinalValue - STARTING_CASH;

        double spPnlPct =
                (spPnl / STARTING_CASH) * 100;

        Label title = new Label("Game Over!");

        title.setStyle(
                "-fx-font-size: 28px;"
                        + "-fx-font-weight: bold;"
        );

        Label finalLabel =
                new Label(
                        String.format(
                                "Final Portfolio Value: $%.2f",
                                finalValue
                        )
                );

        Label pnlLabel =
                new Label(
                        String.format(
                                "Your P&L: %s$%.2f  (%s%.1f%%)",
                                pnl >= 0 ? "+" : "",
                                pnl,
                                pnl >= 0 ? "+" : "",
                                pnlPct
                        )
                );

        pnlLabel.setStyle(
                "-fx-font-size: 15px;"
                        + "-fx-font-weight: bold;"
                        + "-fx-text-fill: "
                        + (pnl >= 0 ? "#2e7d32" : "#c62828")
                        + ";"
        );

        Label spLabel =
                new Label(
                        String.format(
                                "Incremental SPY Strategy: $%.2f  |  P&L: %s$%.2f  (%s%.1f%%)",
                                spFinalValue,
                                spPnl >= 0 ? "+" : "",
                                spPnl,
                                spPnl >= 0 ? "+" : "",
                                spPnlPct
                        )
                );

        Label vsLabel =
                new Label(
                        getSPComparisonText(pnl, spPnl)
                );

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

        HBox buttons =
                new HBox(
                        12,
                        playAgainButton,
                        homeButton
                );

        buttons.setAlignment(Pos.CENTER);

        // Initialize the summary comparison graph
        graphInterface.showSummaryComparison(
                "Your Portfolio vs Incremental SPY Strategy",
                getGameDateList(),
                playerValueHistory,
                spIncrementalValues
        );

        VBox content =
                new VBox(
                        14,
                        title,
                        new Separator(),
                        finalLabel,
                        pnlLabel,
                        spLabel,
                        vsLabel,
                        graphInterface.getView(),
                        new Separator(),
                        buttons
                );

        // Make the graph grow to fill available space
        VBox.setVgrow(graphInterface.getView(), Priority.ALWAYS);

        content.setPadding(new Insets(30));

        BorderPane root = new BorderPane(content);

        BorderPane.setAlignment(content, Pos.CENTER);

        return root;
    }

    private List<Double> calculateIncrementalSPValues() {

        List<StockRecord> spRecords = getSPRecords();

        List<Double> values = new ArrayList<>();

        if (spRecords.isEmpty()) {
            values.add(STARTING_CASH);
            return values;
        }

        double cashPerDay =
                STARTING_CASH / spRecords.size();

        double shares = 0.0;
        double cash = STARTING_CASH;

        for (StockRecord record : spRecords) {

            double price = record.getClose();

            if (price > 0 && cash >= cashPerDay) {

                shares += cashPerDay / price;
                cash -= cashPerDay;
            }

            double currentValue =
                    cash + (shares * price);

            values.add(currentValue);
        }

        return values;
    }

    private List<StockRecord> getSPRecords() {

        Map<String, List<StockRecord>> allRecords =
                loadAllRecords();

        List<StockRecord> spyRecords =
                allRecords.get(SP_SYMBOL);

        if (spyRecords == null || spyRecords.isEmpty()) {
            return new ArrayList<>();
        }

        spyRecords.sort(null);

        List<LocalDate> gameDates = getGameDateList();

        Map<LocalDate, StockRecord> spyByDate =
                new HashMap<>();

        for (StockRecord record : spyRecords) {
            spyByDate.put(record.getDate(), record);
        }

        List<StockRecord> matchedRecords =
                new ArrayList<>();

        for (LocalDate date : gameDates) {

            StockRecord record = spyByDate.get(date);

            if (record != null) {
                matchedRecords.add(record);
            }
        }

        return matchedRecords;
    }

    private List<LocalDate> getGameDateList() {

        List<LocalDate> dates = new ArrayList<>();

        if (selectedSymbols == null || selectedSymbols.isEmpty()) {
            return dates;
        }

        String firstSymbol = selectedSymbols.get(0);

        List<StockRecord> records =
                gameRecords.get(firstSymbol);

        if (records == null) {
            return dates;
        }

        for (StockRecord record : records) {
            dates.add(record.getDate());
        }

        return dates;
    }

    private String getSPComparisonText(
            double playerPnl,
            double spPnl) {

        if (playerPnl > spPnl) {
            return "You beat the incremental SPY strategy!";
        }

        if (playerPnl < spPnl) {
            return "The incremental SPY strategy beat your portfolio this time.";
        }

        return "You matched the incremental SPY strategy exactly!";
    }

    private Map<String, List<StockRecord>> loadAllRecords() {

        try {

            return new StockDataFileManager(
                    StockInfo.STOCK_DATA_FILE
            ).loadAllRecords();

        } catch (IOException e) {

            e.printStackTrace();
            return new HashMap<>();
        }
    }
}