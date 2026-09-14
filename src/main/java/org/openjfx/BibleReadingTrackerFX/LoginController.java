package org.openjfx.BibleReadingTrackerFX;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Optional;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Button registerButton;
    @FXML private Button forgotPasswordButton;

    private UserManager userManager = new UserManager();

    @FXML
    public void initialize() {
        loginButton.setOnAction(event -> login());
        registerButton.setOnAction(event -> register());
        forgotPasswordButton.setOnAction(event -> showForgotPasswordDialog());
    }

    private void login() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert("Error", "Username and password cannot be empty.");
            return;
        }

        if (userManager.validateUser(username, password)) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("BibleReader.fxml"));
                Parent root = loader.load();
                BibleReaderController controller = loader.getController();
                controller.setUser(username);

                Stage stage = (Stage) loginButton.getScene().getWindow();
                stage.setScene(new Scene(root, 1280, 700));
                stage.setTitle("Bible Reader - " + username);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            showAlert("Error", "Invalid username or password.");
        }
    }

    private void register() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert("Error", "Username and password cannot be empty.");
            return;
        }

        if (userManager.registerUser(username, password)) {
            showAlert("Success", "Registration successful. You can now login.");
        } else {
            showAlert("Error", "Registration failed. The username might be taken.");
        }
    }
    
    private void showForgotPasswordDialog() {
        TextField usernameInput = new TextField();
        usernameInput.setPromptText("Username");
        
        PasswordField newPasswordInput = new PasswordField();
        newPasswordInput.setPromptText("New Password");
        
        VBox vbox = new VBox(10, 
            new Label("Enter your username:"),
            usernameInput,
            new Label("Enter new password:"),
            newPasswordInput
        );
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Reset Password");
        alert.setHeaderText("Password Reset");
        alert.getDialogPane().setContent(vbox);
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String username = usernameInput.getText();
            String newPassword = newPasswordInput.getText();
            
            if (userManager.resetPassword(username, newPassword)) {
                showAlert("Success", "Password updated successfully!");
            } else {
                showAlert("Error", "Password reset failed. User may not exist.");
            }
        }
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}