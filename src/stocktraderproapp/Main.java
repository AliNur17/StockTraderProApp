package stocktraderproapp;

import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    public static final String HOME = "home";
    public static final String STOCK_SEARCH = "stockSearch";
    public static final String ABOUT = "about";

    @Override
    public void start(Stage stage) {
        ScreenManager manager = new ScreenManager(stage);

        stage.setTitle("StockTraderProApp");
        manager.show(HOME);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}