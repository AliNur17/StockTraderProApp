package stocktraderproapp;

import java.time.format.DateTimeFormatter;

import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;

public class ChartHoverOverlay {

    private final LineChart<Number, Number> chart;
    private final NumberAxis xAxis;
    private final NumberAxis yAxis;
    private final Pane hoverPane;

    private final Line horizontalLine;
    private final Line verticalLine;
    private final Circle hoverPoint;
    private final Label hoverBox;

    public ChartHoverOverlay(
            LineChart<Number, Number> chart,
            NumberAxis xAxis,
            NumberAxis yAxis) {

        this.chart = chart;
        this.xAxis = xAxis;
        this.yAxis = yAxis;

        horizontalLine = new Line();
        horizontalLine.setManaged(false);
        horizontalLine.setVisible(false);
        horizontalLine.setStyle(
                "-fx-stroke: #777777;"
                        + "-fx-stroke-width: 1;"
                        + "-fx-stroke-dash-array: 6 4;"
        );

        verticalLine = new Line();
        verticalLine.setManaged(false);
        verticalLine.setVisible(false);
        verticalLine.setStyle(
                "-fx-stroke: #777777;"
                        + "-fx-stroke-width: 1;"
                        + "-fx-stroke-dash-array: 6 4;"
        );

        hoverPoint = new Circle(5);
        hoverPoint.setManaged(false);
        hoverPoint.setVisible(false);
        hoverPoint.setStyle(
                "-fx-fill: white;"
                        + "-fx-stroke: black;"
                        + "-fx-stroke-width: 2;"
        );

        hoverBox = new Label();
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

        hoverPane = new Pane();
        hoverPane.setPickOnBounds(true);
        hoverPane.setStyle("-fx-background-color: transparent;");
        hoverPane.getChildren().addAll(horizontalLine, verticalLine, hoverPoint, hoverBox);

        hoverPane.setOnMouseMoved(event -> update(event.getX(), event.getY()));

        hoverPane.setOnMouseExited(event -> {
            horizontalLine.setVisible(false);
            verticalLine.setVisible(false);
            hoverPoint.setVisible(false);
            hoverBox.setVisible(false);
        });
    }

    public Pane getPane() {
        return hoverPane;
    }

    private void update(double mouseX, double mouseY) {

        Node plotBackground = chart.lookup(".chart-plot-background");

        if (plotBackground == null) {
            return;
        }

        Bounds plotBounds = hoverPane.sceneToLocal(
                plotBackground.localToScene(plotBackground.getBoundsInLocal())
        );

        if (!plotBounds.contains(mouseX, mouseY)) {
            hideAll();
            return;
        }

        XYChart.Series<Number, Number> closestSeries = null;
        XYChart.Data<Number, Number> closestData = null;
        double closestX = 0;
        double closestY = 0;
        double smallestDistance = Double.MAX_VALUE;

        for (XYChart.Series<Number, Number> series : chart.getData()) {

            for (XYChart.Data<Number, Number> data : series.getData()) {

                double displayX = plotBounds.getMinX()
                        + xAxis.getDisplayPosition(data.getXValue().doubleValue());

                double displayY = plotBounds.getMinY()
                        + yAxis.getDisplayPosition(data.getYValue().doubleValue());

                double distance = Math.hypot(mouseX - displayX, mouseY - displayY);

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
            hideAll();
            return;
        }

        StockRecord record = (StockRecord) closestData.getExtraValue();

        if (record == null || closestSeries == null) {
            return;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");

        String closeValue = String.format("%.2f", record.getClose());

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
                closestSeries.getName()
                        + "\nDate: " + record.getDate().format(formatter)
                        + "\nClose: $" + closeValue
        );

        hoverBox.applyCss();
        hoverBox.autosize();

        double boxX = closestX + 12;
        double boxY = closestY - 45;

        if (boxX + hoverBox.getWidth() > plotBounds.getMaxX()) {
            boxX = closestX - hoverBox.getWidth() - 12;
        }

        if (boxY < plotBounds.getMinY()) {
            boxY = plotBounds.getMinY() + 5;
        }

        hoverBox.relocate(boxX, boxY);

        horizontalLine.setVisible(true);
        verticalLine.setVisible(true);
        hoverPoint.setVisible(true);
        hoverBox.setVisible(true);
    }

    private void hideAll() {
        horizontalLine.setVisible(false);
        verticalLine.setVisible(false);
        hoverPoint.setVisible(false);
        hoverBox.setVisible(false);
    }
}
