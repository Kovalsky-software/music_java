package org.example.vp_final;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.IOException;

public class MainApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        DatabaseHelper.initDatabase();

        int lastUserId = DatabaseHelper.loadLastLoggedInUserId();
        double[] windowState = DatabaseHelper.loadWindowState();
        double x = windowState[0];
        double y = windowState[1];
        double width = windowState[2];
        double height = windowState[3];

        FXMLLoader mainLoader = new FXMLLoader(
                MainApplication.class.getResource("/org/example/vp_final/main-layout.fxml")
        );
        Scene scene = new Scene(mainLoader.load(), width, height);

        // Получаем главный контроллер
        MainController mainController = mainLoader.getController();

        if (lastUserId != -1) {
            User savedUser = DatabaseHelper.getUserById(lastUserId);
            if (savedUser != null) {
                mainController.setUser(savedUser);  // ← Это передаст пользователя ВО ВСЕ экраны
            }
        }

        stage.setTitle("Моё Приложение");
        stage.setScene(scene);
        stage.setX(x);
        stage.setY(y);
        stage.setWidth(width);
        stage.setHeight(height);
        stage.setResizable(true);
        stage.show();

        saveWindowStateOnClose(stage);
    }

    private void saveWindowStateOnClose(Stage stage) {
        stage.setOnCloseRequest(event -> {
            if (stage.isMaximized()) {
                // Если окно развёрнуто — сохраняем "нормальные" размеры
                DatabaseHelper.saveWindowState(
                        stage.getX(), stage.getY(),
                        stage.getWidth(), stage.getHeight()
                );
            } else {
                DatabaseHelper.saveWindowState(
                        stage.getX(), stage.getY(),
                        stage.getWidth(), stage.getHeight()
                );
            }
        });

        // Также сохраняем при изменении размеров/позиции (опционально, но надёжнее)
        stage.xProperty().addListener((obs, old, newVal) -> saveIfNotAuth(stage));
        stage.yProperty().addListener((obs, old, newVal) -> saveIfNotAuth(stage));
        stage.widthProperty().addListener((obs, old, newVal) -> saveIfNotAuth(stage));
        stage.heightProperty().addListener((obs, old, newVal) -> saveIfNotAuth(stage));
    }

    private void saveIfNotAuth(Stage stage) {
        // Не сохраняем, если это окно авторизации
        if (stage.getTitle().equals("Авторизация")) return;

        DatabaseHelper.saveWindowState(
                stage.getX(),
                stage.getY(),
                stage.getWidth(),
                stage.getHeight()
        );
    }

    public static void main(String[] args) {
        launch();
    }
}