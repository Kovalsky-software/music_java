package org.example.vp_final;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.*;

public class AddTrackController {

    @FXML private TextField titleField;
    @FXML private ComboBox<String> artistComboBox;
    @FXML private TextField albumField;
    @FXML private ComboBox<String> genreComboBox;
    @FXML private Label filePathLabel;
    @FXML private Label durationLabel;
    @FXML private Label statusLabel;

    private File selectedFile;

    @FXML
    private void initialize() {
        loadArtists();
        loadGenres();
    }

    private void loadArtists() {
        artistComboBox.getItems().clear();
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:music_app.db");
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT Name FROM Artist ORDER BY Name")) {
            while (rs.next()) {
                artistComboBox.getItems().add(rs.getString("Name"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void loadGenres() {
        genreComboBox.getItems().clear();
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:music_app.db");
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT Name FROM Genre ORDER BY Name")) {
            while (rs.next()) {
                genreComboBox.getItems().add(rs.getString("Name"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML
    private void chooseFile() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Аудиофайлы", "*.mp3", "*.wav", "*.flac"),
                new FileChooser.ExtensionFilter("Все файлы", "*.*")
        );
        selectedFile = fc.showOpenDialog(null);

        if (selectedFile != null) {
            filePathLabel.setText(selectedFile.getName());
            try {
                AudioFile audioFile = AudioFileIO.read(selectedFile);
                int duration = audioFile.getAudioHeader().getTrackLength();
                durationLabel.setText(String.format("%d:%02d", duration / 60, duration % 60));

                // Автозаполнение метаданными
                Tag tag = audioFile.getTag();
                if (tag != null) {
                    if (titleField.getText().isEmpty()) {
                        titleField.setText(tag.getFirst(FieldKey.TITLE));
                    }
                    String artist = tag.getFirst(FieldKey.ARTIST);
                    if (artist != null && !artistComboBox.getItems().contains(artist)) {
                        artistComboBox.setValue(artist);
                    }
                }
            } catch (Exception e) {
                durationLabel.setText("Не удалось прочитать");
            }
        }
    }

    @FXML
    private void addTrack() {
        if (selectedFile == null) {
            statusLabel.setText("Выберите файл трека!");
            return;
        }

        String title = titleField.getText().trim();
        String artistName = artistComboBox.getValue();
        String album = albumField.getText().trim();
        String genreName = genreComboBox.getValue();

        if (title.isEmpty() || artistName == null || genreName == null) {
            statusLabel.setText("Заполните все обязательные поля!");
            return;
        }

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:music_app.db")) {
            conn.setAutoCommit(false);

            // 1. Получаем или создаём исполнителя
            int artistId = getOrCreateArtist(conn, artistName);

            // 2. Получаем или создаём альбом (если указан)
            Integer albumId = album.isEmpty() ? null : getOrCreateAlbum(conn, album, artistId);

            // 3. Получаем или создаём жанр
            int genreId = getOrCreateGenre(conn, genreName);

            // 4. Копируем файл
            String fileName = System.currentTimeMillis() + "_" + selectedFile.getName();
            Path dest = Path.of("tracks", fileName);
            Files.createDirectories(dest.getParent());
            Files.copy(selectedFile.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);

            // 5. Добавляем трек
            String sql = "INSERT INTO Track (Title, ArtistID, AlbumID, Duration, TrackURL, GenreID) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, title);
                ps.setInt(2, artistId);
                ps.setObject(3, albumId);
                ps.setInt(4, getDurationSeconds(selectedFile));
                ps.setString(5, "tracks/" + fileName);
                ps.setInt(6, genreId);
                ps.executeUpdate();
            }

            conn.commit();
            statusLabel.setStyle("-fx-text-fill: #27ae60;");
            statusLabel.setText("Трек успешно добавлен!");
            clearForm();

        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Ошибка: " + e.getMessage());
        }
    }

    private int getDurationSeconds(File file) throws Exception {
        AudioFile af = AudioFileIO.read(file);
        return af.getAudioHeader().getTrackLength();
    }

    private int getOrCreateArtist(Connection c, String name) throws SQLException {
        String sql = "SELECT ArtistID FROM Artist WHERE LOWER(Name) = LOWER(?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO Artist (Name, Genre) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, "Unknown");
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            rs.next();
            return rs.getInt(1);
        }
    }

    private Integer getOrCreateAlbum(Connection c, String title, int artistId) throws SQLException {
        String sql = "SELECT AlbumID FROM Album WHERE Title = ? AND ArtistID = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setInt(2, artistId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO Album (Title, ArtistID) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, title);
            ps.setInt(2, artistId);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            rs.next();
            return rs.getInt(1);
        }
    }

    private int getOrCreateGenre(Connection c, String name) throws SQLException {
        String sql = "SELECT GenreID FROM Genre WHERE LOWER(Name) = LOWER(?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO Genre (Name) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            rs.next();
            return rs.getInt(1);
        }
    }

    private void clearForm() {
        titleField.clear();
        albumField.clear();
        filePathLabel.setText("Не выбран");
        durationLabel.setText("—");
        selectedFile = null;
    }

    @FXML
    private void closeWindow() {
        ((Stage) titleField.getScene().getWindow()).close();
    }
}