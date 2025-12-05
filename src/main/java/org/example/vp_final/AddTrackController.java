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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.util.Optional; // Добавлено для Optional

public class AddTrackController {

    // --- FXML Элементы ---
    @FXML private TextField titleField;
    @FXML private ComboBox<String> artistComboBox;
    @FXML private TextField albumField;
    @FXML private ComboBox<String> genreComboBox;
    @FXML private Label filePathLabel;
    @FXML private Label durationLabel;
    @FXML private Label statusLabel;

    // --- Переменные состояния ---
    private File selectedFile;
    // Константа для пути к БД (удобно для изменения)
    private static final String DB_URL = "jdbc:sqlite:music_app.db";
    // Константа для директории треков
    private static final String TRACKS_DIR = "tracks";

    // --- Инициализация ---
    @FXML
    private void initialize() {
        // Устанавливаем начальный статус
        statusLabel.setText("Выберите аудиофайл...");

        loadArtists();
        loadGenres();

        // Установка стилей и подсказок
        artistComboBox.setEditable(true);
        genreComboBox.setEditable(true);
        artistComboBox.getEditor().setPromptText("Введите или выберите");
        genreComboBox.getEditor().setPromptText("Введите или выберите");

        // Добавление слушателей для очистки сообщения об ошибке при начале ввода
        titleField.textProperty().addListener((obs, oldV, newV) -> resetStatus());
        artistComboBox.valueProperty().addListener((obs, oldV, newV) -> resetStatus());
        genreComboBox.valueProperty().addListener((obs, oldV, newV) -> resetStatus());
    }

    private void resetStatus() {
        statusLabel.setText("");
        statusLabel.setStyle(null); // Сбросить стиль
    }

    // --- Загрузка данных из БД ---
    private void loadArtists() {
        artistComboBox.getItems().clear();
        // Используем константу DB_URL
        try (Connection c = DriverManager.getConnection(DB_URL);
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT Name FROM Artist ORDER BY Name")) {
            while (rs.next()) {
                artistComboBox.getItems().add(rs.getString("Name"));
            }
        } catch (SQLException e) {
            showError("Ошибка загрузки исполнителей: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadGenres() {
        genreComboBox.getItems().clear();
        // Используем константу DB_URL
        try (Connection c = DriverManager.getConnection(DB_URL);
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT Name FROM Genre ORDER BY Name")) {
            while (rs.next()) {
                genreComboBox.getItems().add(rs.getString("Name"));
            }
        } catch (SQLException e) {
            showError("Ошибка загрузки жанров: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // --- Выбор файла ---
    @FXML
    private void chooseFile() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Аудиофайлы", "*.mp3", "*.wav", "*.flac", "*.ogg", "*.m4a"), // Добавил m4a
                new FileChooser.ExtensionFilter("Все файлы", "*.*")
        );

        // Передаем текущую Stage (окно), чтобы диалоговое окно было модальным
        Stage stage = (Stage) filePathLabel.getScene().getWindow();
        selectedFile = fc.showOpenDialog(stage);

        if (selectedFile != null) {
            filePathLabel.setText(selectedFile.getName());
            resetStatus(); // Сброс статуса

            // Чтение метаданных
            try {
                AudioFile audioFile = AudioFileIO.read(selectedFile);
                int duration = audioFile.getAudioHeader().getTrackLength();
                // Форматирование длительности (уже было корректно)
                durationLabel.setText(String.format("%d:%02d", duration / 60, duration % 60));

                Tag tag = audioFile.getTagOrCreateAndSetDefault();
                if (tag != null && !tag.isEmpty()) {
                    // TITLE
                    String titleFromTag = getTagValue(tag, FieldKey.TITLE);
                    if (!titleFromTag.isEmpty() && titleField.getText().isEmpty()) {
                        titleField.setText(titleFromTag);
                    }

                    // ARTIST
                    String artistFromTag = getTagValue(tag, FieldKey.ARTIST);
                    if (!artistFromTag.isEmpty()) {
                        setComboBoxValue(artistComboBox, artistFromTag);
                    } else if (artistComboBox.getValue() == null) {
                        // Установить пустое значение, если из тега ничего нет и комбобокс пуст
                        artistComboBox.setValue(null);
                    }

                    // ALBUM
                    String albumFromTag = getTagValue(tag, FieldKey.ALBUM);
                    if (!albumFromTag.isEmpty()) {
                        albumField.setText(albumFromTag);
                    }

                    // GENRE
                    String genreFromTag = getTagValue(tag, FieldKey.GENRE);
                    // Удаляем скобки с числами, если есть (например, "Pop (13)")
                    genreFromTag = genreFromTag.replaceAll("\\s*\\(\\d+\\)\\s*", "").trim();
                    if (!genreFromTag.isEmpty()) {
                        setComboBoxValue(genreComboBox, genreFromTag);
                    } else if (genreComboBox.getValue() == null) {
                        genreComboBox.setValue(null);
                    }
                }
            } catch (Exception e) {
                durationLabel.setText("Ошибка чтения");
                showError("Ошибка чтения метаданных: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            selectedFile = null; // Убедиться, что файл не выбран
            filePathLabel.setText("Не выбран");
            durationLabel.setText("—");
        }
    }

    // Вспомогательный метод для более чистого извлечения и обработки тегов
    private String getTagValue(Tag tag, FieldKey key) {
        String value = tag.getFirst(key);
        return (value != null) ? value.trim() : "";
    }

    // Вспомогательный метод для установки значения ComboBox (с добавлением, если нет)
    private void setComboBoxValue(ComboBox<String> comboBox, String value) {
        if (!comboBox.getItems().contains(value)) {
            comboBox.getItems().add(value);
        }
        comboBox.setValue(value);
    }

    // --- Добавление трека ---
    @FXML
    private void addTrack() {
        // Проверка наличия выбранного файла
        if (selectedFile == null) {
            showError("Выберите файл!");
            return;
        }

        // Санитизация и извлечение данных
        String title = titleField.getText().trim();
        // Используем Optional для более безопасной работы с ComboBox.getValue()
        String artistName = Optional.ofNullable(artistComboBox.getValue()).orElse("").trim();
        String album = albumField.getText().trim();
        String genreName = Optional.ofNullable(genreComboBox.getValue()).orElse("").trim();

        // Проверки на обязательные поля
        if (title.isEmpty()) {
            // Если названия нет, используем имя файла (уже было корректно)
            title = selectedFile.getName().replaceAll("\\.\\w+$", ""); // Удаляем расширение файла
        }

        if (artistName.isEmpty()) {
            showError("Укажите исполнителя!");
            return;
        }

        if (genreName.isEmpty()) {
            showError("Укажите жанр!");
            return;
        }

        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            conn.setAutoCommit(false); // Начало транзакции

            try {
                // Если альбом пуст, передаем null, иначе получаем/создаем ID
                int genreId = getOrCreateGenre(conn, genreName);
                int artistId = getOrCreateArtist(conn, artistName, genreId);
                Integer albumId = album.isEmpty() ? null : getOrCreateAlbum(conn, album, artistId);

                // --- Копирование файла ---
                Path tracksDir = Path.of(TRACKS_DIR);
                if (!Files.exists(tracksDir)) {
                    Files.createDirectories(tracksDir); // Используем createDirectories для надежности
                }

                // Создаем уникальное имя файла
                String fileExtension = getFileExtension(selectedFile);
                String uniqueFileName = System.currentTimeMillis() + "_" + title.replaceAll("[^a-zA-Z0-9_.-]", "_") + fileExtension;
                Path dest = tracksDir.resolve(uniqueFileName);

                // Проверяем, что файл не скопирован сам в себя
                if (!selectedFile.toPath().equals(dest)) {
                    Files.copy(selectedFile.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
                } else {
                    // Это маловероятно, но на всякий случай
                    throw new IOException("Исходный и конечный файл совпадают. Операция отменена.");
                }

                String dbPath = TRACKS_DIR + "/" + uniqueFileName;
                int durationSeconds = getDurationSeconds(selectedFile); // Получаем длительность

                // --- Добавление трека в БД ---
                String sql = "INSERT INTO Track (Title, ArtistID, AlbumID, Duration, TrackURL, GenreID) VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, title);
                    ps.setInt(2, artistId);

                    // Правильное использование setNull для AlbumID
                    if (albumId == null) {
                        ps.setNull(3, java.sql.Types.INTEGER);
                    } else {
                        ps.setInt(3, albumId);
                    }

                    ps.setInt(4, durationSeconds);
                    ps.setString(5, dbPath);
                    ps.setInt(6, genreId); // Используем setInt, т.к. GenreID всегда int
                    ps.executeUpdate();
                }

                conn.commit(); // Подтверждение транзакции
                showSuccess("Трек успешно добавлен!");
                clearForm();

            } catch (Exception e) {
                conn.rollback(); // Откат транзакции при ошибке
                showError("Ошибка добавления трека: " + e.getMessage());
                // Если файл был скопирован, пытаемся его удалить при откате
                try {
                    if (Path.of(TRACKS_DIR).resolve(getFileExtension(selectedFile)).toFile().exists()) {
                        Files.deleteIfExists(Path.of(TRACKS_DIR).resolve(getFileExtension(selectedFile)));
                    }
                } catch (IOException ioException) {
                    System.err.println("Не удалось удалить скопированный файл при откате: " + ioException.getMessage());
                }
                e.printStackTrace();
            }

        } catch (SQLException e) {
            showError("Ошибка подключения к БД: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Вспомогательный метод для получения расширения файла
    private String getFileExtension(File file) {
        String name = file.getName();
        int lastIndexOf = name.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return ""; // нет расширения
        }
        return name.substring(lastIndexOf);
    }


    // --- Вспомогательные методы для БД ---

    private int getDurationSeconds(File file) throws Exception {
        // Убрал @throws Exception из сигнатуры, заменил на конкретные (IOException, TagException, ReadOnlyFileException и т.д.)
        // но оставил Exception для совместимости с jaudiotagger
        AudioFile af = AudioFileIO.read(file);
        return af.getAudioHeader().getTrackLength();
    }

    private int getOrCreateArtist(Connection conn, String name, int genreId) throws SQLException {
        if (name == null || name.trim().isEmpty()) {
            name = "Неизвестный исполнитель";
        }
        name = name.trim();

        // 1. Ищем существующего исполнителя
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT ArtistID FROM Artist WHERE LOWER(Name) = LOWER(?)")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int existingId = rs.getInt(1);
                    // Обновляем GenreID, если он пустой или 0
                    try (PreparedStatement updatePs = conn.prepareStatement(
                            "UPDATE Artist SET GenreID = ? WHERE ArtistID = ? AND (GenreID IS NULL OR GenreID = 0)")) {
                        updatePs.setInt(1, genreId);
                        updatePs.setInt(2, existingId);
                        updatePs.executeUpdate();
                    }
                    loadArtists();
                    return existingId;
                }
            }
        }

        // 2. Создаём нового с GenreID
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO Artist (Name, GenreID) VALUES (?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setInt(2, genreId);  // ← Теперь правильно: int!
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int newId = keys.getInt(1);
                    loadArtists();
                    return newId;
                }
            }
        }
        throw new SQLException("Не удалось создать исполнителя: " + name);
    }

    private Integer getOrCreateAlbum(Connection c, String title, int artistId) throws SQLException {
        title = title.trim();
        // 1. Поиск существующего
        // В SQLite сравнение строк по умолчанию не чувствительно к регистру (в отличие от MySQL),
        // но для лучшей совместимости добавил LOWER
        try (PreparedStatement ps = c.prepareStatement("SELECT AlbumID FROM Album WHERE LOWER(Title) = LOWER(?) AND ArtistID = ?")) {
            ps.setString(1, title);
            ps.setInt(2, artistId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }

        // 2. Создание нового
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO Album (Title, ArtistID) VALUES (?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, title);
            ps.setInt(2, artistId);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return null; // Возвращаем null, если не удалось создать альбом (что должно быть перехвачено выше)
    }

    private int getOrCreateGenre(Connection conn, String name) throws SQLException {
        if (name == null || name.isEmpty()) {
            name = "Unknown";
        }
        name = name.trim();

        // 1. Поиск существующего
        try (PreparedStatement ps = conn.prepareStatement("SELECT GenreID FROM Genre WHERE LOWER(Name) = LOWER(?)")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }

        // 2. Создание нового
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO Genre (Name) VALUES (?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    loadGenres(); // Обновляем список жанров в ComboBox
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Не удалось создать жанр: " + name);
    }

    // --- Вспомогательные методы UI ---

    private void showSuccess(String message) {
        statusLabel.setStyle("-fx-text-fill: #27ae60;"); // Зеленый цвет
        statusLabel.setText(message);
    }

    private void showError(String message) {
        statusLabel.setStyle("-fx-text-fill: #c0392b;"); // Красный цвет
        statusLabel.setText(message);
    }


    private void clearForm() {
        titleField.clear();
        albumField.clear();
        filePathLabel.setText("Не выбран");
        durationLabel.setText("—");
        selectedFile = null;
        artistComboBox.setValue(null);
        genreComboBox.setValue(null);
        // Повторная загрузка списков после добавления нового исполнителя/жанра (уже сделано в getOrCreate...)
        // loadArtists();
        // loadGenres();
    }

    @FXML
    private void closeWindow() {
        // Безопасное приведение и закрытие Stage
        Stage stage = (Stage) titleField.getScene().getWindow();
        if (stage != null) {
            stage.close();
        }
    }
}