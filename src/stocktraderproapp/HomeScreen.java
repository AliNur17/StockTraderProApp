package stocktraderproapp;

import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class HomeScreen extends BaseScreen {

    public HomeScreen(ScreenManager manager) {
        super(manager);
    }

    @Override
    public Parent getView() {
        Label title = new Label("StockTraderProApp");
        title.setStyle("-fx-font-size: 32px; -fx-font-weight: bold;");

        Button searchButton = new Button("Search Stock");
        Button watchlistButton = new Button("My Watchlist");
        Button aboutButton = new Button("About");

        searchButton.setOnAction(e -> manager.show(Main.STOCK_SEARCH));
        watchlistButton.setOnAction(e -> manager.show(Main.WATCHLIST));
        aboutButton.setOnAction(e -> manager.show(Main.ABOUT));

        VBox root = new VBox(20, title, searchButton, watchlistButton, aboutButton);
        root.setAlignment(Pos.CENTER);

        return root;
    }
}