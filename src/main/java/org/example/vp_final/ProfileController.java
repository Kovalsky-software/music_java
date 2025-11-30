package org.example.vp_final;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;

public class ProfileController {

    @FXML private Label usernameLabel;
    @FXML private Label idLabel;

    private User currentUser;

    public void setUser(User user) {
        this.currentUser = user;
        usernameLabel.setText(user.username());
        idLabel.setText(String.valueOf(user.id()));
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