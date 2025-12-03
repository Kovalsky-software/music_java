package org.example.vp_final;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent; // Импорт для обработки закрытия

import java.io.IOException;

public class MainApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        DatabaseHelper.initDatabase();

        // 1. Загрузка состояния окна
        double[] state = DatabaseHelper.loadWindowState();

        // 2. Загрузка последнего пользователя
        int lastUserId = DatabaseHelper.loadLastLoggedInUserId();
        User lastUser = DatabaseHelper.getUserById(lastUserId);

        // 3. Выбор начального экрана (Авторизация ИЛИ Главная)
        FXMLLoader loader;
        Scene scene;

        if (lastUser != null) {
            // Пользователь найден, сразу переходим на Главную
            loader = new FXMLLoader(
                    MainApplication.class.getResource("/org/example/vp_final/main-layout.fxml")
            );
            scene = new Scene(loader.load(), state[2], state[3]); // Используем сохраненные размеры

            MainController controller = loader.getController();
            controller.setUser(lastUser); // Устанавливаем пользователя

            stage.setTitle("Музыка");
            stage.setResizable(true); // Разрешаем изменение размера

        } else {
            // Пользователь не найден, показываем экран Авторизации
            loader = new FXMLLoader(
                    MainApplication.class.getResource("/org/example/vp_final/auth-view.fxml")
            );
            scene = new Scene(loader.load(), 420, 550); // Размеры по умолчанию для Auth
            stage.setTitle("Авторизация");
            stage.setResizable(false);
        }

        // 4. Установка сохраненного положения окна
        stage.setX(state[0]);
        stage.setY(state[1]);

        stage.setScene(scene);

        // 5. СОХРАНЕНИЕ СОСТОЯНИЯ ПРИ ЗАКРЫТИИ
        stage.setOnCloseRequest((WindowEvent event) -> {
            DatabaseHelper.saveWindowState(
                    stage.getX(),
                    stage.getY(),
                    stage.getWidth(),
                    stage.getHeight()
            );
        });

        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}