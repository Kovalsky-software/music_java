package org.example.vp_final;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.application.HostServices;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.io.IOException;

public class ProfileController {

    // ВСЕ @FXML ПОЛЯ — ТОЛЬКО В НАЧАЛЕ КЛАССА!
    @FXML private Label usernameLabel;
    @FXML private Label idLabel;
    @FXML private Label aboutArrow;
    @FXML private VBox aboutDetails;

    private User currentUser;
    private boolean aboutExpanded = false;

    // Метод вызывается автоматически после загрузки FXML
    public void setUser(User user) {
        this.currentUser = user;
        usernameLabel.setText(user.username());
        idLabel.setText(String.valueOf(user.userId()));
    }

    @FXML
    private void toggleAboutDetails() {
        aboutExpanded = !aboutExpanded;
        aboutArrow.setText(aboutExpanded ? "Up Arrow" : "Down Arrow");

        aboutDetails.setVisible(aboutExpanded);
        aboutDetails.setManaged(aboutExpanded);
    }

    private HostServices hostServices;

    // Добавь этот метод — его вызовешь из MainApplication
    public void setHostServices(HostServices hostServices) {
        this.hostServices = hostServices;
    }

    @FXML
    private void openGitHubLink() {
        if (hostServices != null) {
            hostServices.showDocument("https://github.com/твой-ник/vp_final");
        }
    }

    @FXML
    private void onLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/org/example/vp_final/auth-view.fxml")
            );
            Scene authScene = new Scene(loader.load(), 420, 550);

            Stage stage = (Stage) usernameLabel.getScene().getWindow();
            stage.setScene(authScene);
            stage.setTitle("Авторизация");
            stage.setResizable(false);
            stage.centerOnScreen();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}