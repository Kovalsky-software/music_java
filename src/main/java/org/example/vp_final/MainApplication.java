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

        if (lastUserId != -1) {
            // Есть сохранённый пользователь → автоматически входим
            User savedUser = DatabaseHelper.getUserById(lastUserId);

            if (savedUser != null) {
                FXMLLoader loader = new FXMLLoader(
                        MainApplication.class.getResource("/org/example/vp_final/home-view.fxml")
                );
                Scene scene = new Scene(loader.load(), 1100, 800);

                HomeController controller = loader.getController();
                controller.setUser(savedUser);

                stage.setTitle("Моё Приложение");
                stage.setScene(scene);
                stage.setResizable(true);
                stage.centerOnScreen();
                stage.show();
                return;
            }
        }

        FXMLLoader loader = new FXMLLoader(
                MainApplication.class.getResource("/org/example/vp_final/auth-view.fxml")
        );
        Scene scene = new Scene(loader.load(), 420, 550);
        stage.setTitle("Авторизация");
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}