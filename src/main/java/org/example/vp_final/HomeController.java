package org.example.vp_final;

import javafx.fxml.FXML;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;

public class HomeController {

    private MainController mainController; // будет установлен из MainController
    private User currentUser;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    public void setUser(User user) {
        this.currentUser = user;
    }

    @FXML
    private void onMainAction() {
        // Здесь будет главное действие приложения
        System.out.println("Запуск основного действия для пользователя: " + currentUser.username());
        // Можно открыть новое окно, модальное окно и т.д.
    }

    @FXML
    private void openProfile(MouseEvent event) {
        if (mainController != null) {
            mainController.showProfile();
        }
    }
}