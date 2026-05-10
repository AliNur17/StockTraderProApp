package stocktraderproapp;

import java.util.HashMap;
import java.util.Map;

import javafx.scene.Scene;
import javafx.stage.Stage;

public class ScreenManager {

    private final Stage stage;
    private final Map<String, AppScreen> screens = new HashMap<>();

    public ScreenManager(Stage stage) {
        this.stage = stage;
        registerScreens();
    }

    private void registerScreens() {
        screens.put(Main.HOME, new HomeScreen(this));
        screens.put(Main.STOCK_SEARCH, new StockSearchScreen(this));
        screens.put(Main.ABOUT, new AboutScreen(this));
        screens.put(Main.WATCHLIST, new WatchlistScreen(this));
    }

    public void show(String screenName) {
        AppScreen screen = screens.get(screenName);

        if (screen == null) {
            throw new IllegalArgumentException("No screen registered for: " + screenName);
        }

        Scene scene = new Scene(screen.getView(), 900, 600);

        if (ScreenManager.class.getResource("application.css") != null) {
            scene.getStylesheets().add(
                ScreenManager.class.getResource("application.css").toExternalForm()
            );
        }

        stage.setScene(scene);
    }
}