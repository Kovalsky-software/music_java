package org.example.vp_final;

import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;

public class BottomNavController {
    private MainController mainController;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML private void switchToHome()    { mainController.showHome(); }
    @FXML private void switchToSearch()  { mainController.showSearch(); }
    @FXML private void switchToProfile() { mainController.showProfile(); }
}