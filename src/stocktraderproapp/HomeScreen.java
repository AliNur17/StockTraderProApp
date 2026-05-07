package stocktraderproapp;

import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class HomeScreen implements AppScreen {

    private final ScreenManager manager;

    public HomeScreen(ScreenManager manager) {
        this.manager = manager;
    }

    @Override
    public Parent getView() {
        Label title = new Label("StockTraderProApp");
        title.setStyle("-fx-font-size: 32px; -fx-font-weight: bold;");

        Button searchButton = new Button("Search Stock");
        Button aboutButton = new Button("About");

        searchButton.setOnAction(e -> manager.show(Main.STOCK_SEARCH));
        aboutButton.setOnAction(e -> manager.show(Main.ABOUT));

        VBox root = new VBox(20, title, searchButton, aboutButton);
        root.setAlignment(Pos.CENTER);

        return root;
    }
}