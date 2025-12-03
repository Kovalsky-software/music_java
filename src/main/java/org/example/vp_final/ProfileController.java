package org.example.vp_final;

import javafx.scene.control.TextField;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.application.HostServices;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.io.IOException;
import java.sql.*;

public class ProfileController {

    // === СТАРЫЕ ПОЛЯ (оставляем как было) ===
    @FXML private Label usernameLabel;
    @FXML private Label idLabel;
    @FXML private Label aboutArrow;
    @FXML private VBox aboutDetails;

    // === НОВОЕ: контейнер для треков "Нравится" ===
    @FXML private FlowPane likedTracksContainer;
    @FXML private FlowPane favoriteAuthorsContainer;

    // === НОВОЕ: поля для плейлистов ===
    @FXML private TextField newPlaylistTitleField;
    @FXML private Label playlistMessageLabel;
    @FXML private FlowPane playlistsContainer;

    private User currentUser;
    private boolean aboutExpanded = false;
    private HostServices hostServices;

    // === СТАРЫЙ МЕТОД — остаётся без изменений ===
    public void setUser(User user) {
        this.currentUser = user;
        usernameLabel.setText(user.username());
        idLabel.setText("ID: " + user.userId());
        loadFavoriteAuthors();
        loadLikedTracks();  // ← Загружаем "Нравится" сразу при входе в профиль
        loadPlaylists();    // ← НОВОЕ: Загружаем плейлисты сразу при входе в профиль
    }

    public void setHostServices(HostServices hostServices) {
        this.hostServices = hostServices;
    }

    @FXML
    private void toggleAboutDetails() {
        aboutExpanded = !aboutExpanded;
        aboutArrow.setText(aboutExpanded ? "Скрыть" : "Показать");
        aboutDetails.setVisible(aboutExpanded);
        aboutDetails.setManaged(aboutExpanded);
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

    private void loadFavoriteAuthors() {
        if (currentUser == null) return;
        favoriteAuthorsContainer.getChildren().clear();

        String sql = """
        SELECT a.ArtistID, a.Name 
        FROM Artist a
        JOIN UserLikeAuthor ula ON a.ArtistID = ula.ArtistID
        WHERE ula.UserID = ?
        """;

        try (Connection c = DriverManager.getConnection("jdbc:sqlite:music_app.db");
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, currentUser.userId());
            ResultSet rs = p.executeQuery();

            while (rs.next()) {
                VBox author = new VBox(12);
                author.setPadding(new Insets(20));
                author.setStyle("-fx-background-color: #e74c3c; -fx-background-radius: 50%; -fx-pref-width: 100; -fx-pref-height: 100; -fx-alignment: center;");
                Label name = new Label(rs.getString("Name"));
                name.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
                author.getChildren().add(name);
                favoriteAuthorsContainer.getChildren().add(author);
            }

            if (favoriteAuthorsContainer.getChildren().isEmpty()) {
                Label empty = new Label("Тут будут ваши любимые исполнители");
                empty.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 16;");
                favoriteAuthorsContainer.getChildren().add(empty);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ===================== НОВАЯ ЧАСТЬ: "Нравится" =====================

    private void loadLikedTracks() {
        if (likedTracksContainer == null || currentUser == null) return;

        likedTracksContainer.getChildren().clear();

        String sql = """
            SELECT t.TrackID, t.Title, a.Name AS ArtistName
            FROM Track t
            LEFT JOIN Artist a ON t.ArtistID = a.ArtistID
            JOIN UserLike ul ON t.TrackID = ul.TrackID
            WHERE ul.UserID = ?
            ORDER BY t.Title
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
                if (artist == null) artist = "Неизвестный артист";

                VBox card = createTrackCard(trackId, title, artist);
                likedTracksContainer.getChildren().add(card);
            }

            if (!hasTracks) {
                Label empty = new Label("Тут будут треки, которые тебе нравятся, " + currentUser.username());
                empty.setStyle("-fx-font-size: 18; -fx-text-fill: #95a5a6; -fx-padding: 50;");
                empty.setWrapText(true);
                likedTracksContainer.getChildren().add(empty);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            Label error = new Label("Ошибка загрузки треков");
            likedTracksContainer.getChildren().add(error);
        }
    }

    private VBox createTrackCard(int trackId, String title, String artist) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(18));
        card.setMinSize(170, 110);
        card.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 16; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 12, 0.2, 0, 4); " +
                "-fx-cursor: hand;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 15; -fx-text-fill: #2c3e50;");
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(140);

        Label artistLabel = new Label(artist);
        artistLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 13;");

        Label removeBtn = new Label("Remove");
        removeBtn.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 18; -fx-cursor: hand;");

        // Удаление по клику на крестик
        removeBtn.setOnMouseClicked(e -> {
            e.consume(); // чтобы не сработало воспроизведение
            removeFromLiked(trackId, card);
        });

        card.getChildren().addAll(titleLabel, artistLabel, removeBtn);

        // Воспроизведение по клику на карточку (но не на крестик)
        card.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
            if (!e.getTarget().equals(removeBtn)) {
                System.out.println("Воспроизвести из профиля: " + title + " — " + artist);
                // Здесь потом вызовешь плеер
            }
        });

        return card;
    }

    private void removeFromLiked(int trackId, VBox card) {
        String sql = "DELETE FROM UserLike WHERE UserID = ? AND TrackID = ?";

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:music_app.db");
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, currentUser.userId());
            pstmt.setInt(2, trackId);
            pstmt.executeUpdate();

            likedTracksContainer.getChildren().remove(card);

            // Если стало пусто — показываем заглушку
            if (likedTracksContainer.getChildren().isEmpty()) {
                Label empty = new Label("Тут будут треки, которые тебе нравятся, " + currentUser.username() + " (Musical note)");
                empty.setStyle("-fx-font-size: 18; -fx-text-fill: #95a5a6; -fx-padding: 50;");
                likedTracksContainer.getChildren().add(empty);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ===================== НОВАЯ ЧАСТЬ: "Мои плейлисты" =====================

    private void loadPlaylists() {
        if (currentUser == null) return;
        playlistsContainer.getChildren().clear();

        String sql = "SELECT PlaylistID, Title, CreationDate FROM Playlist WHERE UserID = ? ORDER BY CreationDate DESC";

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:music_app.db");
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, currentUser.userId());
            try (ResultSet rs = pstmt.executeQuery()) {
                boolean hasPlaylists = false;
                while (rs.next()) {
                    hasPlaylists = true;
                    int id = rs.getInt("PlaylistID");
                    String title = rs.getString("Title");
                    String date = rs.getString("CreationDate");
                    VBox card = createPlaylistCard(id, title, date);
                    playlistsContainer.getChildren().add(card);
                }
                if (!hasPlaylists) {
                    Label empty = new Label("Создайте свой первый плейлист!");
                    empty.setStyle("-fx-font-size: 16; -fx-text-fill: #95a5a6; -fx-padding: 20;");
                    playlistsContainer.getChildren().add(empty);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            playlistMessageLabel.setText("Ошибка загрузки плейлистов");
        }
    }

    private VBox createPlaylistCard(int id, String title, String date) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 12; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 8, 0, 0, 2); " +
                "-fx-pref-width: 200; -fx-alignment: top-left; -fx-cursor: hand;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label dateLabel = new Label("Создан: " + (date != null ? date.substring(0, 10) : "Неизвестно"));
        dateLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #7f8c8d;");

        Label deleteBtn = new Label("Удалить");
        deleteBtn.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12; -fx-cursor: hand;");

        deleteBtn.setOnMouseClicked(e -> {
            e.consume();
            removePlaylist(id, card);
        });

        card.getChildren().addAll(titleLabel, dateLabel, deleteBtn);

        card.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
            if (!e.getTarget().equals(deleteBtn)) {
                System.out.println("Открыть плейлист ID: " + id + " (" + title + ")");
                // Здесь добавьте логику открытия плейлиста (например, новый экран с треками плейлиста)
            }
        });

        return card;
    }

    private void removePlaylist(int id, VBox card) {
        String sql = "DELETE FROM Playlist WHERE PlaylistID = ? AND UserID = ?"; // Добавляем UserID для безопасности

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:music_app.db");
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            pstmt.setInt(2, currentUser.userId());
            pstmt.executeUpdate();

            playlistsContainer.getChildren().remove(card);

            if (playlistsContainer.getChildren().isEmpty()) {
                Label empty = new Label("Создайте свой первый плейлист!");
                empty.setStyle("-fx-font-size: 16; -fx-text-fill: #95a5a6; -fx-padding: 20;");
                playlistsContainer.getChildren().add(empty);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            playlistMessageLabel.setText("Ошибка удаления плейлиста");
        }
    }

    @FXML
    private void onCreateNewPlaylist() {
        String title = newPlaylistTitleField.getText().trim();
        if (title.isEmpty()) {
            playlistMessageLabel.setText("Введите название плейлиста");
            return;
        }

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:music_app.db")) {
            // Проверка на уникальность названия для этого пользователя
            String checkSql = "SELECT 1 FROM Playlist WHERE UserID = ? AND Title = ?";
            try (PreparedStatement checkPstmt = conn.prepareStatement(checkSql)) {
                checkPstmt.setInt(1, currentUser.userId());
                checkPstmt.setString(2, title);
                if (checkPstmt.executeQuery().next()) {
                    playlistMessageLabel.setText("Плейлист с таким названием уже существует");
                    return;
                }
            }

            // Создание плейлиста
            String insertSql = "INSERT INTO Playlist (UserID, Title) VALUES (?, ?)";
            try (PreparedStatement insertPstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                insertPstmt.setInt(1, currentUser.userId());
                insertPstmt.setString(2, title);
                insertPstmt.executeUpdate();

                try (ResultSet generatedKeys = insertPstmt.getGeneratedKeys()) {
                    int newId = generatedKeys.next() ? generatedKeys.getInt(1) : -1;

                    // Получить дату создания
                    String dateSql = "SELECT CreationDate FROM Playlist WHERE PlaylistID = ?";
                    try (PreparedStatement datePstmt = conn.prepareStatement(dateSql)) {
                        datePstmt.setInt(1, newId);
                        try (ResultSet dateRs = datePstmt.executeQuery()) {
                            String 	CreationDate = dateRs.next() ? dateRs.getString(1) : "Теперь";

                            // Добавить карточку в начало
                            VBox card = createPlaylistCard(newId, title, CreationDate);
                            if (!playlistsContainer.getChildren().isEmpty() && playlistsContainer.getChildren().get(0) instanceof Label) {
                                playlistsContainer.getChildren().clear(); // Убрать заглушку
                            }
                            playlistsContainer.getChildren().add(0, card);

                            newPlaylistTitleField.clear();
                            playlistMessageLabel.setText("");
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            playlistMessageLabel.setText("Ошибка создания плейлиста");
        }
    }
}