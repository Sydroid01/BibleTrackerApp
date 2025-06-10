package org.openjfx.BibleReadingTrackerFX;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class BibleReaderApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        // Load FXML with proper path
        FXMLLoader loader = new FXMLLoader(getClass().getResource("BibleReader.fxml"));
        Parent root = loader.load();
        
        Scene scene = new Scene(root, 800, 700);
        stage.setTitle("Bible Reader");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}