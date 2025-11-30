package org.example.vp_final;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class HomeController {
    @FXML private Label welcomeText;
    private User currentUser;

    public void setUser(User user) {
        this.currentUser = user;
        welcomeText.setText("Привет, " + user.username() + "!\nДобро пожаловать домой!");
    }

    @FXML
    private void onHelloButtonClick() {
        welcomeText.setText("Кнопка нажата!\nПривет ещё раз, " + currentUser.username() + "!");
    }
}