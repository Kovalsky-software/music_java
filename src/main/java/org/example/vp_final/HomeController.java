package org.example.vp_final;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import java.net.URL;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ResourceBundle;

public class HomeController implements Initializable {

    @FXML private FlowPane newTracksContainer;  // Это FlowPane из home-view.fxml в разделе "Новое"
    @FXML private FlowPane favoriteTracksContainer;

    private User currentUser;
    private MainController mainController;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }
    public void setUser(User user) {
        this.currentUser = user;
        loadFavoriteTracks();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadLatestTracks();
    }

    private void showPlaceholder(String text) {
        Label placeholder = new Label(text);
        placeholder.setStyle("-fx-font-size: 18; -fx-text-fill: #95a5a6; -fx-padding: 40 0 0 0;");
        placeholder.setWrapText(true);
        favoriteTracksContainer.getChildren().add(placeholder);
    }

    private void loadLatestTracks() {
        String sql = """
        SELECT t.TrackID, t.Title, a.Name AS ArtistName
        FROM Track t
        LEFT JOIN Artist a ON t.ArtistID = a.ArtistID
        ORDER BY t.TrackID DESC
        LIMIT 7
        """;

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:music_app.db");
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            newTracksContainer.getChildren().clear();

            while (rs.next()) {
                int trackId = rs.getInt("TrackID");
                String title = rs.getString("Title");
                String artist = rs.getString("ArtistName");
                if (artist == null) artist = "Неизвестный артист";

                VBox trackCard = createTrackCard(trackId, title, artist);
                newTracksContainer.getChildren().add(trackCard);
            }

        } catch (Exception e) {
            e.printStackTrace();
            newTracksContainer.getChildren().add(new Label("Ошибка загрузки треков"));
        }
    }

    private void loadFavoriteTracks() {
        favoriteTracksContainer.getChildren().clear();

        if (currentUser == null) {
            showPlaceholder("Войдите в аккаунт");
            return;
        }

        String sql = """
        SELECT t.TrackID, t.Title, a.Name AS ArtistName, t.Duration
        FROM Track t
        JOIN Artist a ON t.ArtistID = a.ArtistID
        JOIN UserLike ul ON t.TrackID = ul.TrackID
        WHERE ul.UserID = ?
        ORDER BY t.Title   -- сортируем просто по названию трека (или можно по TrackID)
        """;

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:music_app.db");
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, currentUser.userId());
            ResultSet rs = pstmt.executeQuery();

            boolean hasTracks = false;

            while (rs.next()) {
                hasTracks = true;

                int trackId = rs.getInt("TrackID");
                String title = rs.getString("Title");
                String artist = rs.getString("ArtistName");
                if (artist == null) artist = "Неизвестный исполнитель";

                VBox card = createTrackCard(trackId, title, artist);
                favoriteTracksContainer.getChildren().add(card);
            }

            if (!hasTracks) {
                showPlaceholder("Тут будут твои любимые треки, " + currentUser.username() + " [Heart]");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showPlaceholder("Ошибка загрузки любимых треков");
        }
    }

    private VBox createTrackCard(int trackId, String title, String artist) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(16));
        card.setStyle("""
        -fx-background-color: #ffffff;
        -fx-background-radius: 12;
        -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 3);
        -fx-cursor: hand;
        """);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 15; -fx-text-fill: #2c3e50;");

        Label artistLabel = new Label(artist);
        artistLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 13;");

        card.getChildren().addAll(titleLabel, artistLabel);

        // Теперь title и artist — параметры метода → они effectively final → можно использовать в лямбде
        card.setOnMouseClicked(e -> {
            System.out.println("Воспроизвести трек ID=" + trackId + ": " + title + " — " + artist);
            playTrack(trackId, title, artist); // потом можно будет сюда передать плеер
        });

        return card;
    }

    // Заглушка для будущего плеера
    private void playTrack(int trackId, String title, String artist) {
        System.out.println("Плеер: сейчас играет → " + title + " (" + artist + ")");
        // Здесь потом откроешь модальное окно с плеером, передашь trackId и т.д.
    }

    @FXML
    private void onMainAction() {
        System.out.println("Запуск основного действия для пользователя: " +
                (currentUser != null ? currentUser.username() : "гость"));
    }

    @FXML
    private void openProfile() {
        if (mainController != null) {
            mainController.showProfile();
        }
    }
}