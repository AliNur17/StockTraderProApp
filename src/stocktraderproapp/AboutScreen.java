package stocktraderproapp;

import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class AboutScreen extends BaseScreen {

    public AboutScreen(ScreenManager manager) {
        super(manager);
    }

    @Override
    public Parent getView() {
        Label title = new Label("About StockTraderProApp");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold;");

        Label description = new Label(
            "StockTraderProApp is a JavaFX stock viewer designed to display closing price trends."
        );
        description.setWrapText(true);
        description.setMaxWidth(450);

        Button backButton = new Button("Back");
        backButton.setOnAction(e -> manager.show(Main.HOME));

        VBox root = new VBox(20, title, description, backButton);
        root.setAlignment(Pos.CENTER);

        return root;
    }
}