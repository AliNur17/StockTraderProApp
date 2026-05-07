package stocktraderproapp;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;

import org.json.JSONObject;

public class StockSearchScreen implements AppScreen {

    private final ScreenManager manager;

    private static final String API_KEY = "DPTNWD8PD2WLDQ0E";

    public StockSearchScreen(ScreenManager manager) {
        this.manager = manager;
    }

    @Override
    public Parent getView() {

        TextField searchField = new TextField();
        searchField.setPromptText("Enter stock symbol, e.g. AAPL");

        Button searchButton = new Button("Search");
        Button backButton = new Button("Back");

        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();

        xAxis.setLabel("Trading Day");
        yAxis.setLabel("Closing Price ($)");

        xAxis.setAutoRanging(false);
        yAxis.setAutoRanging(false);

        LineChart<Number, Number> chart =
                new LineChart<>(xAxis, yAxis);
        chart.setPadding(new Insets(20,40,20,40));

        chart.setTitle("Recent Closing Prices");
        chart.setCreateSymbols(false);
        chart.setAnimated(false);

        searchButton.setOnAction(e -> {

            String symbol =
                    searchField.getText()
                               .trim()
                               .toUpperCase();

            if (symbol.isEmpty()) {
                return;
            }

            chart.getData().clear();

            searchButton.setDisable(true);
            searchButton.setText("Loading...");

            Task<XYChart.Series<Number, Number>> task =
                    new Task<>() {

                @Override
                protected XYChart.Series<Number, Number>
                call() throws Exception {

                    String urlString =
                            "https://www.alphavantage.co/query?"
                            + "function=TIME_SERIES_DAILY"
                            + "&symbol=" + symbol
                            + "&outputsize=compact"
                            + "&apikey=" + API_KEY;

                    URL url = new URL(urlString);

                    HttpURLConnection connection =
                            (HttpURLConnection) url.openConnection();

                    connection.setRequestMethod("GET");

                    BufferedReader reader =
                            new BufferedReader(
                                    new InputStreamReader(
                                            connection.getInputStream()
                                    )
                            );

                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }

                    reader.close();

                    JSONObject json =
                            new JSONObject(response.toString());

                    if (!json.has("Time Series (Daily)")) {
                        throw new Exception(
                                "No stock data found. "
                                + "Check your API key, symbol, or request limit."
                        );
                    }

                    JSONObject timeSeries =
                            json.getJSONObject("Time Series (Daily)");

                    XYChart.Series<Number, Number> series =
                            new XYChart.Series<>();

                    series.setName(symbol);

                    List<String> dates =
                            new ArrayList<>(timeSeries.keySet());

                    Collections.sort(dates);
                    if (!dates.isEmpty()) {
                        dates.remove(dates.size() - 1);
                    }

                    List<String> xLabels =
                            new ArrayList<>();

                    DateTimeFormatter formatter =
                            DateTimeFormatter.ofPattern("MMM dd");

                    double maxClose = 0;
                    int dayIndex = 0;

                    for (String date : dates) {

                        JSONObject dailyData =
                                timeSeries.getJSONObject(date);

                        double close =
                                Double.parseDouble(
                                        dailyData.getString("4. close")
                                );

                        if (close > maxClose) {
                            maxClose = close;
                        }

                        LocalDate parsedDate =
                                LocalDate.parse(date);

                        String formattedDate =
                                parsedDate.format(formatter);

                        xLabels.add(formattedDate);

                        series.getData().add(
                                new XYChart.Data<>(
                                        dayIndex,
                                        close
                                )
                        );

                        dayIndex++;
                    }

                    final int finalDayIndex = dayIndex;

                    xAxis.setLowerBound(0);

                    xAxis.setUpperBound(finalDayIndex-1);

                    int tickSpacing =
                            Math.max(1, finalDayIndex / 10);

                    xAxis.setTickUnit(tickSpacing);

                    yAxis.setLowerBound(0);
                    yAxis.setUpperBound(maxClose * 2);
                    yAxis.setTickUnit(maxClose / 10);

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

                                
                                    if (index == finalDayIndex
                                            && !xLabels.isEmpty()) {

                                        return xLabels.get(
                                                xLabels.size() - 1
                                        );
                                    }

                                    return "";
                                }

                                @Override
                                public Number fromString(String string) {
                                    return 0;
                                }
                            }
                    );

                    return series;
                }
            };

            task.setOnSucceeded(event -> {

                chart.getData().add(task.getValue());

                searchButton.setDisable(false);
                searchButton.setText("Search");
            });

            task.setOnFailed(event -> {

                task.getException().printStackTrace();

                searchButton.setDisable(false);
                searchButton.setText("Search");
            });

            Thread thread = new Thread(task);
            thread.setDaemon(true);
            thread.start();
        });

        backButton.setOnAction(
                e -> manager.show(Main.HOME)
        );

        HBox topBar =
                new HBox(
                        10,
                        searchField,
                        searchButton,
                        backButton
                );

        topBar.setPadding(new Insets(15));

        BorderPane root = new BorderPane();

        root.setTop(topBar);
        root.setCenter(chart);

        return root;
    }
}