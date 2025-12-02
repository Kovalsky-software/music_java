package org.example.vp_final;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import java.net.URL;
import java.sql.*;
import java.util.ResourceBundle;

public class SearchController implements Initializable {

    @FXML private HBox genresContainer;
    @FXML private VBox tracksSection;
    @FXML private Label genreTitleLabel;
    @FXML private VBox tracksContainer;
    @FXML private VBox artistsSection;
    @FXML private FlowPane artistsContainer;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadGenres();
    }

    private void loadGenres() {
        String sql = "SELECT GenreID, Name, Description FROM Genre ORDER BY Name";

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:music_app.db");
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                int genreId = rs.getInt("GenreID");
                String name = rs.getString("Name");
                String desc = rs.getString("Description");

                VBox card = createGenreCard(genreId, name, desc);
                genresContainer.getChildren().add(card);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private VBox createGenreCard(int genreId, String name, String description) {
        VBox card = new VBox(14);
        card.setPadding(new Insets(28, 24, 28, 24));
        card.setPrefSize(180, 180);           // ← вот так лучше: фиксированный размер
        card.setMaxSize(180, 180);
        card.setAlignment(Pos.CENTER_LEFT);

        card.setStyle(getCardStyle(name) +
                "-fx-background-radius: 24; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 20, 0.3, 0, 8);");

        // Эффект наведения (опционально)
        card.setOnMouseEntered(e -> card.setScaleX(1.06));
        card.setOnMouseExited(e -> card.setScaleX(1.0));
        card.setCursor(Cursor.HAND);

        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 21; -fx-font-weight: bold;");

        String descText = description != null && !description.isEmpty() ? description : "Слушать сейчас";
        Label descLabel = new Label(descText);
        descLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.92); -fx-font-size: 14;");
        descLabel.setWrapText(true);

        card.getChildren().addAll(nameLabel, descLabel);
        card.setOnMouseClicked(e -> showTracksByGenre(genreId, name));

        return card;
    }

    private String getCardStyle(String genre) {
        return switch (genre) {
            case "Поп" -> "-fx-background-color: #e91e63;";
            case "Рок" -> "-fx-background-color: #9c27b0;";
            case "Хип-хоп" -> "-fx-background-color: #ff9800;";
            case "Электронная" -> "-fx-background-color: #00bcd4;";
            case "Классика" -> "-fx-background-color: #795548;";
            case "Джаз" -> "-fx-background-color: #3f51b5;";
            case "Ретро" -> "-fx-background-color: #e91e63;";
            case "Лофи" -> "-fx-background-color: #4caf50;";
            default -> "-fx-background-color: #607d8b;";
        } + "-fx-background-radius: 16; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 4);";
    }

    private void showTracksByGenre(int genreId, String genreName) {
        genreTitleLabel.setText("Треки жанра: " + genreName);
        tracksContainer.getChildren().clear();
        artistsContainer.getChildren().clear();

        // Показываем оба раздела
        tracksSection.setVisible(true);
        tracksSection.setManaged(true);
        artistsSection.setVisible(true);
        artistsSection.setManaged(true);

        // === Загрузка треков ===
        String trackSql = """
        SELECT t.Title, a.Name AS ArtistName
        FROM Track t
        LEFT JOIN Artist a ON t.ArtistID = a.ArtistID
        WHERE t.GenreID = ?
        ORDER BY t.Title
        """;

        // === Загрузка исполнителей по жанру (через колонку ArtistGenre) ===
        String artistSql = """
        SELECT ArtistID, Name
        FROM Artist
        WHERE ArtistGenre = (SELECT Name FROM Genre WHERE GenreID = ?)
        ORDER BY Name
        """;

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:music_app.db")) {

            // Треки
            try (PreparedStatement ps = conn.prepareStatement(trackSql)) {
                ps.setInt(1, genreId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String title = rs.getString("Title");
                        String artist = rs.getString("ArtistName");
                        if (artist == null) artist = "Неизвестный";

                        HBox row = new HBox(15);
                        row.setPadding(new Insets(12));
                        row.setStyle("-fx-background-color: white; -fx-background-radius: 10;");
                        row.setAlignment(Pos.CENTER_LEFT);

                        row.getChildren().addAll(
                                new Label(title),
                                new Region() {{ HBox.setHgrow(this, Priority.ALWAYS); }},
                                new Label(artist)
                        );

                        tracksContainer.getChildren().add(row);
                    }
                }
            }

            // Исполнители
            try (PreparedStatement ps = conn.prepareStatement(artistSql)) {
                ps.setInt(1, genreId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String name = rs.getString("Name");

                        VBox card = new VBox(10);
                        card.setPadding(new Insets(20));
                        card.setMinWidth(140);
                        card.setStyle("-fx-background-color: #3498db; -fx-background-radius: 16; -fx-cursor: hand;");

                        Label nameLabel = new Label(name);
                        nameLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15;");

                        card.getChildren().add(nameLabel);
                        card.setOnMouseClicked(e -> System.out.println("Открыть исполнителя: " + name));

                        artistsContainer.getChildren().add(card);
                    }

                    if (artistsContainer.getChildren().isEmpty()) {
                        Label empty = new Label("Исполнителей в этом жанре пока нет");
                        empty.setStyle("-fx-text-fill: #95a5a6; -fx-padding: 20;");
                        artistsContainer.getChildren().add(empty);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String formatDuration(int seconds) {
        int m = seconds / 60;
        int s = seconds % 60;
        return String.format("%d:%02d", m, s);
    }

    private static int getGenreId(Connection conn, String name) throws SQLException {
        String sql = "SELECT GenreID FROM Genre WHERE Name = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 1; // fallback
    }
}