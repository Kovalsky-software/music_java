package org.example.vp_final;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import java.io.IOException;

public class MainController {

    @FXML private StackPane contentArea;

    // ВАЖНО: получаем включённый bottom-nav через @FXML + fx:id
    @FXML private BottomNavController bottomNavController;

    private Node homeView, searchView, profileView;
    private User currentUser;

    private HomeController homeController;
    private ProfileController profileController;

    @FXML
    private void initialize() throws IOException {
        // Загружаем все три экрана
        FXMLLoader homeLoader = new FXMLLoader(getClass().getResource("/org/example/vp_final/home-view.fxml"));
        homeView = homeLoader.load();
        homeController = homeLoader.getController();
        homeController.setMainController(this);

        FXMLLoader searchLoader = new FXMLLoader(getClass().getResource("/org/example/vp_final/search-view.fxml"));
        searchView = searchLoader.load();

        FXMLLoader profileLoader = new FXMLLoader(getClass().getResource("/org/example/vp_final/profile-view.fxml"));
        profileView = profileLoader.load();
        profileController = profileLoader.getController();

        // Передаём пользователю данные
        if (currentUser != null) {
            homeController.setUser(currentUser);
            profileController.setUser(currentUser);
        }

        // Подключаем нижнее меню к главному контроллеру
        bottomNavController.setMainController(this);

        // Открываем домашний экран по умолчанию
        showHome();
    }

    public void setUser(User user) {
        this.currentUser = user;
        if (homeController != null) homeController.setUser(user);
        if (profileController != null) profileController.setUser(user);
    }

    public void showHome() {
        contentArea.getChildren().setAll(homeView);
    }

    public void showSearch() {
        contentArea.getChildren().setAll(searchView);
    }

    public void showProfile() {
        contentArea.getChildren().setAll(profileView);
    }
}