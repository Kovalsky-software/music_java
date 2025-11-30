package org.example.vp_final;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        DatabaseHelper.initDatabase();

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