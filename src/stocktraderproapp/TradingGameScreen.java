package stocktraderproapp;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class TradingGameScreen extends BaseScreen {

    private enum GameState { SETUP, PLAYING, SUMMARY }

    private static final double STARTING_CASH = 10_000.0;

    private GameState gameState = GameState.SETUP;
    private Portfolio portfolio;
    private int currentDayIndex;
    private int timeWindow;
    private List<String> selectedSymbols;
    private Map<String, List<StockRecord>> gameRecords;

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
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        Label subtitle = new Label(
                "Select stocks from your watchlist to trade:"
        );

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
        startButton.setStyle("-fx-font-size: 13px;");

        Button backButton = new Button("Back");
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

        VBox content = new VBox(16,
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
        content.setMaxWidth(520);

        return new BorderPane(content);
    }

    private void initGame(
            List<String> chosen,
            Map<String, List<StockRecord>> allRecords,
            int window) {

        TreeSet<LocalDate> commonDates = null;

        for (String symbol : chosen) {

            List<StockRecord> records = allRecords.get(symbol);
            TreeSet<LocalDate> dates = new TreeSet<>();

            for (StockRecord r : records) {
                dates.add(r.getDate());
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

            for (StockRecord r : allRecords.get(symbol)) {
                byDate.put(r.getDate(), r);
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
        timeWindow = gameDates.size();
        currentDayIndex = 0;
        portfolio = new Portfolio(STARTING_CASH);
        gameState = GameState.PLAYING;
    }

    private Parent buildGameView() {

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd, yyyy");

        Map<String, StockRecord> todayRecords = new HashMap<>();
        Map<String, StockRecord> yesterdayRecords = new HashMap<>();

        for (String symbol : selectedSymbols) {

            List<StockRecord> records = gameRecords.get(symbol);

            if (records != null && currentDayIndex < records.size()) {
                todayRecords.put(symbol, records.get(currentDayIndex));
            }

            if (currentDayIndex > 0
                    && records != null
                    && currentDayIndex - 1 < records.size()) {
                yesterdayRecords.put(symbol, records.get(currentDayIndex - 1));
            }
        }

        Map<String, Double> currentPrices = new HashMap<>();

        for (Map.Entry<String, StockRecord> e : todayRecords.entrySet()) {
            currentPrices.put(e.getKey(), e.getValue().getClose());
        }

        String dateStr = todayRecords.isEmpty() ? ""
                : todayRecords.values().iterator().next().getDate().format(fmt);

        Label dayLabel = new Label(
                "Day " + (currentDayIndex + 1) + " / " + timeWindow
                + "   " + dateStr
        );
        dayLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        Label cashLabel = new Label();
        Label portfolioLabel = new Label();

        Runnable refreshHeader = () -> {
            cashLabel.setText(String.format("Cash: $%.2f", portfolio.getCash()));
            double total = portfolio.getTotalValue(currentPrices);
            double pnl = total - STARTING_CASH;
            portfolioLabel.setText(String.format(
                    "Portfolio: $%.2f  (%s$%.2f)",
                    total, pnl >= 0 ? "+" : "", pnl
            ));
            portfolioLabel.setStyle(
                    "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: "
                    + (pnl >= 0 ? "#2e7d32" : "#c62828") + ";"
            );
        };

        refreshHeader.run();

        HBox header = new HBox(24, dayLabel, cashLabel, portfolioLabel);
        header.setPadding(new Insets(12, 16, 12, 16));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle(
                "-fx-background-color: #f0f0f0;"
                + "-fx-border-color: #dddddd;"
                + "-fx-border-width: 0 0 1 0;"
        );

        VBox stockRows = new VBox(0);

        for (String symbol : selectedSymbols) {

            StockRecord today = todayRecords.get(symbol);
            StockRecord yesterday = yesterdayRecords.get(symbol);

            if (today == null) {
                continue;
            }

            double price = today.getClose();

            String changeStr = "";
            boolean positive = true;

            if (yesterday != null && yesterday.getClose() != 0) {
                double pct = (price - yesterday.getClose())
                        / yesterday.getClose() * 100;
                positive = pct >= 0;
                changeStr = String.format(
                        "%s%.2f%%", pct >= 0 ? "+" : "", pct
                );
            }

            String company = StockInfo.COMPANY_NAMES.getOrDefault(symbol, "");

            Label symbolLabel = new Label(symbol);
            symbolLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
            symbolLabel.setPrefWidth(65);

            Label companyLabel = new Label(company);
            companyLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 11px;");
            companyLabel.setPrefWidth(145);

            Label priceLabel = new Label(String.format("$%.2f", price));
            priceLabel.setPrefWidth(80);

            Label changeLabel = new Label(changeStr);
            changeLabel.setStyle(
                    "-fx-text-fill: " + (positive ? "#2e7d32" : "#c62828") + ";"
            );
            changeLabel.setPrefWidth(80);

            Label sharesLabel = new Label();
            sharesLabel.setPrefWidth(80);

            Label valueLabel = new Label();
            valueLabel.setPrefWidth(80);

            Button buyBtn = new Button("Buy 1");
            buyBtn.setStyle("-fx-base: #c8e6c9;");

            Button sellBtn = new Button("Sell 1");
            sellBtn.setStyle("-fx-base: #ffcdd2;");

            Runnable refreshRow = () -> {
                int shares = portfolio.getShares(symbol);
                sharesLabel.setText(shares + " share" + (shares == 1 ? "" : "s"));
                valueLabel.setText(String.format("$%.2f", shares * price));
                buyBtn.setDisable(portfolio.getCash() < price);
                sellBtn.setDisable(shares <= 0);
            };

            refreshRow.run();

            buyBtn.setOnAction(ev -> {
                portfolio.buy(symbol, price);
                refreshRow.run();
                refreshHeader.run();
            });

            sellBtn.setOnAction(ev -> {
                portfolio.sell(symbol, price);
                refreshRow.run();
                refreshHeader.run();
            });

            HBox row = new HBox(12,
                    symbolLabel, companyLabel, priceLabel, changeLabel,
                    sharesLabel, valueLabel, buyBtn, sellBtn
            );
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(10, 16, 10, 16));
            row.setStyle(
                    "-fx-border-color: #eeeeee;"
                    + "-fx-border-width: 0 0 1 0;"
            );

            stockRows.getChildren().add(row);
        }

        ScrollPane scrollPane = new ScrollPane(stockRows);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        boolean isLastDay = currentDayIndex >= timeWindow - 1;

        Button nextButton = new Button(isLastDay ? "Finish Game" : "Next Day >");
        nextButton.setStyle("-fx-font-size: 13px;");
        nextButton.setOnAction(e -> {
            if (isLastDay) {
                gameState = GameState.SUMMARY;
            } else {
                currentDayIndex++;
            }
            manager.show(Main.TRADING_GAME);
        });

        Button quitButton = new Button("Quit Game");
        quitButton.setOnAction(e -> {
            gameState = GameState.SETUP;
            manager.show(Main.TRADING_GAME);
        });

        HBox footer = new HBox(12, quitButton, nextButton);
        footer.setPadding(new Insets(12, 16, 12, 16));
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setStyle(
                "-fx-background-color: #f0f0f0;"
                + "-fx-border-color: #dddddd;"
                + "-fx-border-width: 1 0 0 0;"
        );

        BorderPane root = new BorderPane();
        root.setTop(header);
        root.setCenter(scrollPane);
        root.setBottom(footer);

        return root;
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
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold;");

        Label finalLabel = new Label(
                String.format("Final Portfolio Value: $%.2f", finalValue)
        );
        finalLabel.setStyle("-fx-font-size: 16px;");

        Label pnlLabel = new Label(String.format(
                "Your P&L: %s$%.2f  (%s%.1f%%)",
                pnl >= 0 ? "+" : "", pnl, pnl >= 0 ? "+" : "", pnlPct
        ));
        pnlLabel.setStyle(
                "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: "
                + (pnl >= 0 ? "#2e7d32" : "#c62828") + ";"
        );

        Label buyHoldLabel = new Label(String.format(
                "Buy & Hold P&L: %s$%.2f",
                bhPnl >= 0 ? "+" : "", bhPnl
        ));
        buyHoldLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #555555;");

        String vsText;
        if (pnl > bhPnl)     vsText = "You beat buy & hold!";
        else if (pnl < bhPnl) vsText = "Buy & hold beat you this time.";
        else                   vsText = "You matched buy & hold exactly!";

        Label vsLabel = new Label(vsText);
        vsLabel.setStyle("-fx-font-size: 13px; -fx-font-style: italic;");

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

        VBox content = new VBox(16,
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

        return new BorderPane(content);
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
