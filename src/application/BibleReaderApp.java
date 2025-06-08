package application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class BibleReaderApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        // Load FXML with proper path
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/application/BibleReader.fxml"));
        Parent root = loader.load();
        
        Scene scene = new Scene(root, 1000, 700);
        Image icon = new Image("icon.png");
        stage.getIcons().add(icon);
        stage.setTitle("Bible Reader");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}