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

    // Кнопки (если у вас есть в FXML)
    // @FXML private Button loginButton;   // ← если есть — раскомментируйте

    @FXML
    private void onLogin() {
        authenticate(false);
    }

    @FXML
    private void onRegister() {
        authenticate(true);
    }

    private void authenticate(boolean isRegister) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showMessage("Заполните все поля!", true);
            return;
        }
        if (username.length() < 3 || password.length() < 4) {
            showMessage("Логин ≥3, пароль ≥4 символа", true);
            return;
        }

        if (isRegister) {
            // Временный email — потом сделаете поле
            boolean success = DatabaseHelper.registerUser(username, username + "@example.com", password);
            if (success) {
                showMessage("Регистрация успешна! Вход выполнен.", false);
                // Автоматически логиним после регистрации
                User user = DatabaseHelper.loginUser(username, password);
                if (user != null) {
                    DatabaseHelper.saveLastLoggedInUser(user.userId());
                    openMainScreen(user);
                }
            } else {
                showMessage("Пользователь уже существует!", true);
            }
            return;
        }

        // === ВХОД ===
        User user = DatabaseHelper.loginUser(username, password);
        if (user != null) {
            DatabaseHelper.saveLastLoggedInUser(user.userId()); // ← автологин
            showMessage("Вход успешен!", false);
            openMainScreen(user);
        } else {
            showMessage("Неверный логин или пароль!", true);
        }
    }

    private void openMainScreen(User user) {
        try {
            // Загружаем сохранённые размеры и позицию окна
            double[] windowState = DatabaseHelper.loadWindowState();
            double width = windowState[2];
            double height = windowState[3];

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/org/example/vp_final/home-view.fxml")  // ← ваш главный экран
            );
            Scene scene = new Scene(loader.load(), width, height);

            HomeController controller = loader.getController();
            controller.setUser(user);

            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Моё Приложение");
            stage.setResizable(true);
            stage.centerOnScreen(); // можно оставить, или убрать — как хотите
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showMessage("Ошибка загрузки приложения", true);
        }
    }

    // Универсальный метод для сообщений
    private void showMessage(String text, boolean isError) {
        messageLabel.setText(text);
        messageLabel.setTextFill(isError
                ? javafx.scene.paint.Color.web("#e74c3c")
                : javafx.scene.paint.Color.LIMEGREEN);
    }
}