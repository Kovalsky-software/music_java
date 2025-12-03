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
            // Теперь нужно запрашивать email тоже! Пока можно захардкодить или добавить поле.
            // Пример с временным email:
            if (DatabaseHelper.registerUser(username, username + "@example.com", password)) {
                messageLabel.setTextFill(javafx.scene.paint.Color.LIMEGREEN);
                messageLabel.setText("Регистрация успешна!");
            } else {
                messageLabel.setTextFill(javafx.scene.paint.Color.web("#e74c3c"));
                messageLabel.setText("Пользователь или email уже существует!");
                return;
            }
        }

        User user = DatabaseHelper.loginUser(username, password);
        if (user != null) {
            // *** КЛЮЧЕВОЕ ИЗМЕНЕНИЕ (1): Сохраняем ID пользователя ***
            DatabaseHelper.saveLastLoggedInUser(user.userId());
            openMainScreen(user);
        } else {
            messageLabel.setTextFill(javafx.scene.paint.Color.web("#e74c3c"));
            messageLabel.setText("Неверный логин или пароль!");
        }
    }

    private void openMainScreen(User user) {
        try {
            // *** КЛЮЧЕВОЕ ИЗМЕНЕНИЕ (2): Загружаем сохраненное состояние окна ***
            double[] state = DatabaseHelper.loadWindowState(); // [x, y, width, height]

            FXMLLoader loader = new FXMLLoader(
                    MainApplication.class.getResource("/org/example/vp_final/main-layout.fxml")
            );
            // *** КЛЮЧЕВОЕ ИЗМЕНЕНИЕ (3): Используем сохраненные размеры ***
            Scene scene = new Scene(loader.load(), state[2], state[3]);

            MainController controller = loader.getController();
            controller.setUser(user);

            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Музыка");

            // *** КЛЮЧЕВОЕ ИЗМЕНЕНИЕ (4): Разрешаем изменение размера ***
            stage.setResizable(true);

            // *** КЛЮЧЕВОЕ ИЗМЕНЕНИЕ (5): УДАЛЯЕМ centerOnScreen() ***
            // stage.centerOnScreen();

            // stage.setX(state[0]); // Эти строки не нужны, т.к. stage уже получил x/y от MainApplication
            // stage.setY(state[1]);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}