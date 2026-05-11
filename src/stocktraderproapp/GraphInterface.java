package stocktraderproapp;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.util.StringConverter;

public interface GraphInterface {

    Node getView();

    void clear(String title);

    void showFullRecords(
            String title,
            Map<String, List<StockRecord>> recordsBySymbol,
            List<String> symbols
    );

    void showRecordsThroughDay(
            String title,
            String symbol,
            List<StockRecord> records,
            int currentDayIndex
    );

    void showSummaryComparison(
            String title,
            List<LocalDate> dates,
            List<Double> playerValues,
            List<Double> spyValues
    );
}

class SummaryDataPoint {

    private final LocalDate date;
    private final double value;

    public SummaryDataPoint(LocalDate date, double value) {
        this.date = date;
        this.value = value;
    }

    public LocalDate getDate() {
        return date;
    }

    public double getValue() {
        return value;
    }
}

class StockGraph implements GraphInterface {

    private final NumberAxis xAxis;
    private final NumberAxis yAxis;
    private final LineChart<Number, Number> chart;
    private final StackPane view;
    private final SymbolColorManager colorManager;

    public StockGraph() {

        colorManager = new SymbolColorManager();

        xAxis = new NumberAxis();
        yAxis = new NumberAxis();

        xAxis.setLabel("Date");
        yAxis.setLabel("Value ($)");

        xAxis.setAutoRanging(false);
        yAxis.setAutoRanging(false);

        chart = new LineChart<>(xAxis, yAxis);

        chart.setTitle("Select a Stock");
        chart.setCreateSymbols(false);
        chart.setAnimated(false);
        chart.setLegendVisible(false);
        chart.setPadding(new Insets(20, 40, 10, 40));

        Pane hoverPane = new ChartHoverOverlay(chart, xAxis, yAxis).getPane();

        view = new StackPane(chart, hoverPane);
    }

    @Override
    public Node getView() {
        return view;
    }

    @Override
    public void clear(String title) {
        chart.getData().clear();
        chart.setTitle(title);
        chart.setLegendVisible(false);
    }

    @Override
    public void showFullRecords(
            String title,
            Map<String, List<StockRecord>> recordsBySymbol,
            List<String> symbols) {

        chart.getData().clear();
        chart.setLegendVisible(symbols != null && symbols.size() > 1);

        if (recordsBySymbol == null || symbols == null || symbols.isEmpty()) {
            clear("Select a Stock");
            return;
        }

        List<LocalDate> axisDates = buildAxisDates(recordsBySymbol, symbols);

        if (axisDates.isEmpty()) {
            clear("No saved TXT data found");
            return;
        }

        Map<LocalDate, Integer> dateIndexMap = buildDateIndexMap(axisDates);

        double minClose = Double.MAX_VALUE;
        double maxClose = -Double.MAX_VALUE;

        for (String symbol : symbols) {

            List<StockRecord> records = recordsBySymbol.get(symbol);

            if (records == null || records.isEmpty()) {
                continue;
            }

            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            series.setName(symbol);

            for (StockRecord record : records) {

                Integer xIndex = dateIndexMap.get(record.getDate());

                if (xIndex == null) {
                    continue;
                }

                XYChart.Data<Number, Number> point =
                        new XYChart.Data<>(xIndex, record.getClose());

                point.setExtraValue(record);

                series.getData().add(point);

                minClose = Math.min(minClose, record.getClose());
                maxClose = Math.max(maxClose, record.getClose());
            }

            if (!series.getData().isEmpty()) {
                chart.getData().add(series);
            }
        }

        if (chart.getData().isEmpty()) {
            clear("No saved TXT data found for selected stock");
            return;
        }

        configureXAxisFromDates(axisDates);
        configureYAxis(minClose, maxClose);

        chart.setTitle(title);

        Platform.runLater(this::applyStableColors);
    }

    @Override
    public void showRecordsThroughDay(
            String title,
            String symbol,
            List<StockRecord> records,
            int currentDayIndex) {

        chart.getData().clear();
        chart.setLegendVisible(false);

        if (symbol == null || records == null || records.isEmpty()) {
            clear("Select a Stock");
            return;
        }

        int safeDayIndex = Math.min(
                Math.max(currentDayIndex, 0),
                records.size() - 1
        );

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName(symbol);

        double minClose = Double.MAX_VALUE;
        double maxClose = -Double.MAX_VALUE;

        for (int i = 0; i <= safeDayIndex; i++) {

            StockRecord record = records.get(i);

            XYChart.Data<Number, Number> point =
                    new XYChart.Data<>(i, record.getClose());

            point.setExtraValue(record);

            series.getData().add(point);

            minClose = Math.min(minClose, record.getClose());
            maxClose = Math.max(maxClose, record.getClose());
        }

        chart.getData().add(series);

        configureXAxisFromRecords(records);
        configureYAxis(minClose, maxClose);

        chart.setTitle(title);

        Platform.runLater(this::applyStableColors);
    }

    @Override
    public void showSummaryComparison(
            String title,
            List<LocalDate> dates,
            List<Double> playerValues,
            List<Double> spyValues) {

        chart.getData().clear();
        chart.setLegendVisible(true);

        if (dates == null || dates.isEmpty()
                || playerValues == null || playerValues.isEmpty()
                || spyValues == null || spyValues.isEmpty()) {

            clear("No summary data available");
            return;
        }

        int count = Math.min(
                dates.size(),
                Math.min(playerValues.size(), spyValues.size())
        );

        if (count == 0) {
            clear("No summary data available");
            return;
        }

        XYChart.Series<Number, Number> playerSeries = new XYChart.Series<>();
        playerSeries.setName("Your Portfolio");

        XYChart.Series<Number, Number> spySeries = new XYChart.Series<>();
        spySeries.setName("Incremental SPY Strategy");

        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;

        for (int i = 0; i < count; i++) {

            double playerValue = playerValues.get(i);
            double spyValue = spyValues.get(i);
            LocalDate date = dates.get(i);

            XYChart.Data<Number, Number> playerPoint =
                    new XYChart.Data<>(i, playerValue);

            playerPoint.setExtraValue(
                    new SummaryDataPoint(date, playerValue)
            );

            XYChart.Data<Number, Number> spyPoint =
                    new XYChart.Data<>(i, spyValue);

            spyPoint.setExtraValue(
                    new SummaryDataPoint(date, spyValue)
            );

            playerSeries.getData().add(playerPoint);
            spySeries.getData().add(spyPoint);

            min = Math.min(min, playerValue);
            min = Math.min(min, spyValue);

            max = Math.max(max, playerValue);
            max = Math.max(max, spyValue);
        }

        chart.getData().add(playerSeries);
        chart.getData().add(spySeries);
        chart.setLegendVisible(false);

        configureXAxisFromDates(dates.subList(0, count));
        configureYAxis(min, max);

        chart.setTitle(title);

        Platform.runLater(this::applySummaryColors);
    }

    private List<LocalDate> buildAxisDates(
            Map<String, List<StockRecord>> recordsBySymbol,
            List<String> symbols) {

        TreeSet<LocalDate> dates = new TreeSet<>();

        for (String symbol : symbols) {

            List<StockRecord> records = recordsBySymbol.get(symbol);

            if (records == null) {
                continue;
            }

            for (StockRecord record : records) {
                dates.add(record.getDate());
            }
        }

        return new ArrayList<>(dates);
    }

    private Map<LocalDate, Integer> buildDateIndexMap(
            List<LocalDate> axisDates) {

        Map<LocalDate, Integer> map = new HashMap<>();

        for (int i = 0; i < axisDates.size(); i++) {
            map.put(axisDates.get(i), i);
        }

        return map;
    }

    private void configureXAxisFromDates(List<LocalDate> axisDates) {

        List<String> labels = new ArrayList<>();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd");

        for (LocalDate date : axisDates) {
            labels.add(date.format(formatter));
        }

        int finalDayIndex = axisDates.size();

        xAxis.setLowerBound(0);
        xAxis.setUpperBound(Math.max(0, finalDayIndex - 1));
        xAxis.setTickUnit(Math.max(1, finalDayIndex / 10));

        xAxis.setTickLabelFormatter(
                new StringConverter<Number>() {

                    @Override
                    public String toString(Number value) {

                        int index = value.intValue();

                        if (index >= 0 && index < labels.size()) {
                            return labels.get(index);
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

    private void configureXAxisFromRecords(List<StockRecord> records) {

        int finalDayIndex = records.size();

        xAxis.setLowerBound(0);
        xAxis.setUpperBound(Math.max(0, finalDayIndex - 1));
        xAxis.setTickUnit(Math.max(1, finalDayIndex / 8));

        xAxis.setTickLabelFormatter(
                new StringConverter<Number>() {

                    private final DateTimeFormatter formatter =
                            DateTimeFormatter.ofPattern("MMM dd");

                    @Override
                    public String toString(Number value) {

                        int index = value.intValue();

                        if (index >= 0 && index < records.size()) {
                            return records.get(index)
                                    .getDate()
                                    .format(formatter);
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

    private void configureYAxis(double minValue, double maxValue) {

        if (minValue == Double.MAX_VALUE || maxValue == -Double.MAX_VALUE) {
            minValue = 0;
            maxValue = 1;
        }

        if (Math.abs(maxValue - minValue) < 0.01) {
            maxValue += 1;
            minValue -= 1;
        }

        double padding = Math.max(1, (maxValue - minValue) * 0.20);

        yAxis.setLowerBound(Math.max(0, minValue - padding));
        yAxis.setUpperBound(maxValue + padding);
        yAxis.setTickUnit(Math.max(1, (maxValue - minValue) / 8));
    }

    private void applyStableColors() {

        for (XYChart.Series<Number, Number> series : chart.getData()) {

            String color = colorManager.getColorForSymbol(series.getName());

            Node seriesNode = series.getNode();

            if (seriesNode == null) {
                continue;
            }

            Node line = seriesNode.lookup(".chart-series-line");

            if (line != null) {
                line.setStyle(
                        "-fx-stroke: " + color + ";"
                                + "-fx-stroke-width: 2.5px;"
                );
            }
        }
    }

    private void applySummaryColors() {

        if (chart.getData().size() < 2) {
            return;
        }

        setSeriesColor(0, "#2e7d32");
        setSeriesColor(1, "#1565c0");
    }

    private void setSeriesColor(int index, String color) {

        if (index < 0 || index >= chart.getData().size()) {
            return;
        }

        Node seriesNode = chart.getData().get(index).getNode();

        if (seriesNode == null) {
            return;
        }

        Node line = seriesNode.lookup(".chart-series-line");

        if (line != null) {
            line.setStyle(
                    "-fx-stroke: " + color + ";"
                            + "-fx-stroke-width: 2.8px;"
            );
        }
    }
}