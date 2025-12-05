package org.example.vp_final;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import java.io.IOException;

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

        artistComboBox.setEditable(true);
        genreComboBox.setEditable(true);

        // Опционально: подсказка, если поле пустое
        artistComboBox.getEditor().setPromptText("Введите или выберите");
        genreComboBox.getEditor().setPromptText("Введите или выберите");
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
                new FileChooser.ExtensionFilter("Аудиофайлы", "*.mp3", "*.wav", "*.flac", "*.ogg"),
                new FileChooser.ExtensionFilter("Все файлы", "*.*")
        );
        selectedFile = fc.showOpenDialog(null);

        if (selectedFile != null) {
            filePathLabel.setText(selectedFile.getName());

            try {
                AudioFile audioFile = AudioFileIO.read(selectedFile);
                int duration = audioFile.getAudioHeader().getTrackLength();
                durationLabel.setText(String.format("%d:%02d", duration / 60, duration % 60));

                Tag tag = audioFile.getTagOrCreateAndSetDefault();
                if (tag != null && !tag.isEmpty()) {
                    // Название
                    String titleFromTag = tag.getFirst(FieldKey.TITLE);
                    if (titleFromTag != null && !titleFromTag.isBlank() && titleField.getText().isEmpty()) {
                        titleField.setText(titleFromTag.trim());
                    }

                    // Исполнитель — САМОЕ ГЛАВНОЕ!
                    String artistFromTag = tag.getFirst(FieldKey.ARTIST);
                    if (artistFromTag != null && !artistFromTag.isBlank()) {
                        artistFromTag = artistFromTag.trim();

                        // Если такой исполнитель уже есть в списке — выбираем его
                        if (artistComboBox.getItems().contains(artistFromTag)) {
                            artistComboBox.setValue(artistFromTag);
                        } else {
                            // Если нет — добавляем в список и выбираем
                            artistComboBox.getItems().add(artistFromTag);
                            artistComboBox.setValue(artistFromTag);
                        }
                    }

                    // Альбом
                    String albumFromTag = tag.getFirst(FieldKey.ALBUM);
                    if (albumFromTag != null && !albumFromTag.isBlank()) {
                        albumField.setText(albumFromTag.trim());
                    }

                    // Жанр
                    String genreFromTag = tag.getFirst(FieldKey.GENRE);
                    if (genreFromTag != null && !genreFromTag.isBlank()) {
                        genreFromTag = genreFromTag.trim().replaceAll("\\(\\d+\\)", "").trim(); // убираем (12)
                        if (!genreComboBox.getItems().contains(genreFromTag)) {
                            genreComboBox.getItems().add(genreFromTag);
                        }
                        genreComboBox.setValue(genreFromTag);
                    }
                }
            } catch (Exception e) {
                durationLabel.setText("Ошибка чтения тегов");
                e.printStackTrace();
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

        if (artistName == null || artistName.trim().isEmpty()) {
            statusLabel.setText("Укажите исполнителя!");
            return;
        }
        if (genreName == null || genreName.trim().isEmpty()) {
            statusLabel.setText("Укажите жанр!");
            return;
        }

        artistName = artistName.trim();
        genreName = genreName.trim();

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:music_app.db")) {
            conn.setAutoCommit(false);

            // 1. Получаем или создаём исполнителя
            int artistId = getOrCreateArtist(conn, artistName);

            // 2. Получаем или создаём альбом (если указан)
            Integer albumId = album.isEmpty() ? null : getOrCreateAlbum(conn, album, artistId);

            // 3. Получаем или создаём жанр
            int genreId = getOrCreateGenre(conn, genreName);

            String fileName = System.currentTimeMillis() + "_" + selectedFile.getName();
            String dbPath;  // ← Объявляем здесь!

            try {
                Path tracksDir = Path.of("tracks");
                if (!Files.exists(tracksDir)) {
                    Files.createDirectory(tracksDir);
                }

                Path dest = tracksDir.resolve(fileName);
                Files.copy(selectedFile.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);

                dbPath = "tracks/" + fileName;  // ← Присваиваем здесь

            } catch (IOException e) {
                e.printStackTrace();
                statusLabel.setText("Ошибка копирования файла!");
                try { conn.rollback(); } catch (Exception ex) { ex.printStackTrace(); }
                return;
            }

            // 5. Добавляем трек в БД
            String sql = "INSERT INTO Track (Title, ArtistID, AlbumID, Duration, TrackURL, GenreID) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, title);
                ps.setInt(2, artistId);
                ps.setObject(3, albumId);
                ps.setInt(4, getDurationSeconds(selectedFile));
                ps.setString(5, dbPath);     // ← Теперь видит!
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

    private int getOrCreateArtist(Connection conn, String name) throws SQLException {
        name = name.trim();
        if (name.isEmpty()) name = "Неизвестный исполнитель";

        // 1. Ищем существующего
        String selectSql = "SELECT ArtistID FROM Artist WHERE LOWER(Name) = LOWER(?)";
        try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("ArtistID");  // ← Нашли — возвращаем
                }
            }
        }

        // 2. Если не нашли — создаём нового
        String insertSql = "INSERT INTO Artist (Name, Genre) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, "Unknown");
            ps.executeUpdate();

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);  // ← ВОЗВРАЩАЕМ НОВЫЙ ID!
                } else {
                    throw new SQLException("Создание исполнителя не вернуло ID");
                }
            }
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

    private int getOrCreateGenre(Connection conn, String name) throws SQLException {
        name = name.trim();
        if (name.isEmpty()) name = "Unknown";

        String selectSql = "SELECT GenreID FROM Genre WHERE LOWER(Name) = LOWER(?)";
        try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("GenreID");
            }
        }

        String insertSql = "INSERT INTO Genre (Name) VALUES (?)";
        try (PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
                else throw new SQLException("Не удалось создать жанр");
            }
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