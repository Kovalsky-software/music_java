package org.example.vp_final;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class AuthController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;

    @FXML private void onLogin()    { authenticate(false); }
    @FXML private void onRegister() { authenticate(true); }

    private void authenticate(boolean register) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Заполните все поля!");
            return;
        }
        if (username.length() < 3 || password.length() < 4) {
            messageLabel.setText("Логин ≥3, пароль ≥4 символа");
            return;
        }

        if (register) {
            if (DatabaseHelper.registerUser(username, password)) {
                messageLabel.setTextFill(javafx.scene.paint.Color.LIMEGREEN);
                messageLabel.setText("Регистрация успешна!");
            } else {
                messageLabel.setTextFill(javafx.scene.paint.Color.web("#e74c3c"));
                messageLabel.setText("Такой пользователь уже есть!");
                return;
            }
        }

        User user = DatabaseHelper.loginUser(username, password);
        if (user != null) {
            openMainScreen(user);
        } else {
            messageLabel.setTextFill(javafx.scene.paint.Color.web("#e74c3c"));
            messageLabel.setText("Неверный логин или пароль!");
        }
    }

    private void openMainScreen(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(MainApplication.class.getResource("hello-view.fxml"));
            Scene scene = new Scene(loader.load(), 600, 400);

            MainController controller = loader.getController();
            controller.setUser(user);

            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Главная — " + user.username());
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}