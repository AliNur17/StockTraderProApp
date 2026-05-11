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
        Label title = new Label("StockTraderProApp");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold;");

        Label description = new Label(
            "A JavaFX stock viewer for browsing closing price trends, "
            + "comparing multiple stocks, and managing a personal watchlist. "
            + "Stock data is fetched from the Alpha Vantage API and cached locally."
        );
        description.setWrapText(true);
        description.setMaxWidth(500);
        description.setStyle("-fx-text-fill: #444444;");

        Label courseLabel = new Label("CS 151 — Object-Oriented Design");
        courseLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 10 0 4 0;");

        Label teamLabel = new Label("Team: Collaborative Coding Project");
        teamLabel.setStyle("-fx-font-size: 12px;");

        Label membersTitle = new Label("Members:");
        membersTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");

        Label members = new Label(
            "  Tuan Nguyen\n  Justin Lai\n  Alisher Nurmatov\n  Hruday Prathipati"
        );
        members.setStyle("-fx-font-size: 12px;");

        Button backButton = new Button("Back");
        backButton.setOnAction(e -> manager.show(Main.HOME));

        VBox root = new VBox(12,
                title,
                description,
                courseLabel,
                teamLabel,
                membersTitle,
                members,
                backButton
        );
        root.setAlignment(Pos.CENTER_LEFT);
        root.setPadding(new javafx.geometry.Insets(40));
        root.setMaxWidth(560);

        javafx.scene.layout.BorderPane wrapper = new javafx.scene.layout.BorderPane(root);
        return wrapper;
    }
}