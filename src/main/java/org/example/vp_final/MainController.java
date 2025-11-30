package org.example.vp_final;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.StackPane;
import javafx.scene.Node;

import java.io.IOException;

public class MainController {
    @FXML private StackPane contentArea;
    private Node homeView, searchView, profileView;
    private User currentUser;

    private HomeController homeController;
    private ProfileController profileController;

    @FXML
    private void initialize() throws IOException {
        FXMLLoader homeLoader = new FXMLLoader(
                getClass().getResource("/org/example/vp_final/home-view.fxml")
        );
        homeView = homeLoader.load();
        homeController = homeLoader.getController();

        FXMLLoader searchLoader = new FXMLLoader(
                getClass().getResource("/org/example/vp_final/search-view.fxml")
        );
        searchView = searchLoader.load();

        FXMLLoader profileLoader = new FXMLLoader(
                getClass().getResource("/org/example/vp_final/profile-view.fxml")
        );
        profileView = profileLoader.load();
        profileController = profileLoader.getController();

        // Нижнее меню
        FXMLLoader navLoader = new FXMLLoader(
                getClass().getResource("/org/example/vp_final/bottom-nav.fxml")
        );
        Node bottomNav = navLoader.load();
        BottomNavController navController = navLoader.getController();
        navController.setMainController(this);

        showHome();
    }

    public void setUser(User user) {
        this.currentUser = user;
        homeController.setUser(user);
        profileController.setUser(user);
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