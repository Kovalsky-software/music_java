package org.example.vp_final;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.VBox;
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
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class AddTrackController {

    // --- FXML Элементы ---
    @FXML private TextField titleField;
    @FXML private ComboBox<String> artistComboBox;
    @FXML private TextField albumField;
    @FXML private ComboBox<String> genreComboBox;
    @FXML private Label filePathLabel;
    @FXML private Label durationLabel;
    @FXML private Label statusLabel;
    @FXML private ToggleButton modeToggle;
    @FXML private VBox classicMode, tableMode;
    @FXML private TableView<TrackRow> tracksTable;

    private final ObservableList<TrackRow> tableData = FXCollections.observableArrayList();
    private boolean isTableMode = false;

    // --- Переменные состояния ---
    private File selectedFile;
    private static final String DB_URL = "jdbc:sqlite:music_app.db";
    private static final String TRACKS_DIR = "tracks";

    // Хранилище для новых файлов (путь -> File)
    private final Map<String, File> newFiles = new HashMap<>();

    // --- Инициализация ---
    @FXML
    private void initialize() {
        statusLabel.setText("Выберите аудиофайл...");
        loadArtists();
        loadGenres();
        artistComboBox.setEditable(true);
        genreComboBox.setEditable(true);
        artistComboBox.getEditor().setPromptText("Введите или выберите");
        genreComboBox.getEditor().setPromptText("Введите или выберите");

        titleField.textProperty().addListener((obs, oldV, newV) -> resetStatus());
        artistComboBox.valueProperty().addListener((obs, oldV, newV) -> resetStatus());
        genreComboBox.valueProperty().addListener((obs, oldV, newV) -> resetStatus());

        // --- TableView режим ---
        initializeTableView();
        loadAllTracksToTable();

        // Переключение режимов
        modeToggle.selectedProperty().addListener((obs, oldVal, newVal) -> {
            isTableMode = newVal;
            classicMode.setVisible(!isTableMode);
            classicMode.setManaged(!isTableMode);
            tableMode.setVisible(isTableMode);
            tableMode.setManaged(isTableMode);
            modeToggle.setText(isTableMode ? "Классический режим" : "TableView режим");
        });
    }

    private void initializeTableView() {
        tracksTable.setItems(tableData);
        tracksTable.setEditable(true);

        // Настройка колонок
        TableColumn<TrackRow, Integer> idCol = new TableColumn<>("ID");
        idCol.setPrefWidth(60);
        idCol.setCellValueFactory(new PropertyValueFactory<>("trackId"));

        TableColumn<TrackRow, String> titleCol = new TableColumn<>("Название");
        titleCol.setPrefWidth(280);
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        titleCol.setCellFactory(TextFieldTableCell.forTableColumn());
        titleCol.setEditable(true);
        titleCol.setOnEditCommit(event -> {
            event.getRowValue().titleProperty().set(event.getNewValue());
        });

        TableColumn<TrackRow, String> artistCol = new TableColumn<>("Исполнитель");
        artistCol.setPrefWidth(200);
        artistCol.setCellValueFactory(new PropertyValueFactory<>("artist"));
        artistCol.setCellFactory(TextFieldTableCell.forTableColumn());
        artistCol.setEditable(true);
        artistCol.setOnEditCommit(event -> {
            event.getRowValue().artistProperty().set(event.getNewValue());
        });

        TableColumn<TrackRow, String> albumCol = new TableColumn<>("Альбом");
        albumCol.setPrefWidth(180);
        albumCol.setCellValueFactory(new PropertyValueFactory<>("album"));
        albumCol.setCellFactory(TextFieldTableCell.forTableColumn());
        albumCol.setEditable(true);
        albumCol.setOnEditCommit(event -> {
            event.getRowValue().albumProperty().set(event.getNewValue());
        });

        TableColumn<TrackRow, String> genreCol = new TableColumn<>("Жанр");
        genreCol.setPrefWidth(120);
        genreCol.setCellValueFactory(new PropertyValueFactory<>("genre"));
        genreCol.setCellFactory(TextFieldTableCell.forTableColumn());
        genreCol.setEditable(true);
        genreCol.setOnEditCommit(event -> {
            event.getRowValue().genreProperty().set(event.getNewValue());
        });

        TableColumn<TrackRow, String> durationCol = new TableColumn<>("Длительность");
        durationCol.setPrefWidth(100);
        durationCol.setCellValueFactory(new PropertyValueFactory<>("duration"));

        TableColumn<TrackRow, String> fileCol = new TableColumn<>("Файл");
        fileCol.setPrefWidth(200);
        fileCol.setCellValueFactory(new PropertyValueFactory<>("fileName"));

        tracksTable.getColumns().addAll(idCol, titleCol, artistCol, albumCol, genreCol, durationCol, fileCol);
    }

    private void loadAllTracksToTable() {
        tableData.clear();
        String sql = """
        SELECT t.TrackID, t.Title, a.Name AS ArtistName,
               al.Title AS AlbumTitle, g.Name AS GenreName,
               t.Duration, t.TrackURL
        FROM Track t
        LEFT JOIN Artist a ON t.ArtistID = a.ArtistID
        LEFT JOIN Album al ON t.AlbumID = al.AlbumID
        LEFT JOIN Genre g ON t.GenreID = g.GenreID
        ORDER BY t.TrackID DESC
        """;

        try (Connection c = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String duration = String.format("%d:%02d",
                        rs.getInt("Duration") / 60,
                        rs.getInt("Duration") % 60);

                String fileName = Optional.ofNullable(rs.getString("TrackURL"))
                        .map(p -> Paths.get(p).getFileName().toString())
                        .orElse("—");

                tableData.add(new TrackRow(
                        rs.getInt("TrackID"),
                        rs.getString("Title"),
                        rs.getString("ArtistName"),
                        rs.getString("AlbumTitle"),
                        rs.getString("GenreName"),
                        duration,
                        fileName
                ));
            }
        } catch (SQLException e) {
            showError("Ошибка загрузки треков в таблицу");
            e.printStackTrace();
        }
    }

    @FXML
    private void chooseFileForTable() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Аудио", "*.mp3", "*.wav", "*.flac", "*.ogg", "*.m4a"));
        File file = fc.showOpenDialog(getStage());

        if (file != null) {
            try {
                AudioFile af = AudioFileIO.read(file);
                Tag tag = af.getTagOrCreateAndSetDefault();

                String fileName = file.getName();
                TrackRow row = new TrackRow(
                        0,
                        getTag(tag, FieldKey.TITLE, file.getName().replaceAll("\\.[^.]+$", "")),
                        getTag(tag, FieldKey.ARTIST, "Неизвестный исполнитель"),
                        getTag(tag, FieldKey.ALBUM, ""),
                        getTag(tag, FieldKey.GENRE, "Unknown").replaceAll("\\s*\\(\\d+\\)", ""),
                        String.format("%d:%02d", af.getAudioHeader().getTrackLength() / 60, af.getAudioHeader().getTrackLength() % 60),
                        fileName
                );
                row.setNew(true);

                // Сохраняем файл во временном хранилище
                newFiles.put(fileName, file);

                tableData.add(0, row);
                showSuccess("Файл добавлен в таблицу: " + fileName);
            } catch (Exception e) {
                showError("Ошибка чтения файла: " + e.getMessage());
            }
        }
    }

    private String getTag(Tag tag, FieldKey key, String defaultValue) {
        if (tag == null) return defaultValue;
        String value = tag.getFirst(key);
        return (value != null && !value.trim().isEmpty()) ? value.trim() : defaultValue;
    }

    @FXML
    private void saveTableChanges() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            conn.setAutoCommit(false);

            for (TrackRow row : tableData) {
                if (row.isNew()) {
                    addTrackFromRow(conn, row);
                } else {
                    updateTrackFromRow(conn, row);
                }
            }
            conn.commit();
            newFiles.clear(); // Очищаем хранилище после сохранения
            showSuccess("Все изменения сохранены!");
            loadAllTracksToTable();
        } catch (Exception e) {
            showError("Ошибка сохранения: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void addTrackFromRow(Connection conn, TrackRow row) throws Exception {
        Path destDir = Paths.get(TRACKS_DIR);
        Files.createDirectories(destDir);

        String fileName = row.getFileName();
        File sourceFile = newFiles.get(fileName);

        if (sourceFile == null) {
            throw new Exception("Файл не найден: " + fileName);
        }

        String ext = fileName.substring(fileName.lastIndexOf("."));
        String newName = System.currentTimeMillis() + "_" + row.getTitle().replaceAll("[^a-zA-Z0-9_.-]", "_") + ext;
        Path dest = destDir.resolve(newName);

        Files.copy(sourceFile.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);

        int genreId = getOrCreateGenre(conn, row.getGenre());
        int artistId = getOrCreateArtist(conn, row.getArtist(), genreId);
        Integer albumId = row.getAlbum().isEmpty() ? null : getOrCreateAlbum(conn, row.getAlbum(), artistId);

        String sql = "INSERT INTO Track (Title, ArtistID, AlbumID, Duration, TrackURL, GenreID) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, row.getTitle());
            ps.setInt(2, artistId);
            if (albumId == null) ps.setNull(3, Types.INTEGER); else ps.setInt(3, albumId);
            ps.setInt(4, parseDuration(row.getDuration()));
            ps.setString(5, TRACKS_DIR + "/" + newName);
            ps.setInt(6, genreId);
            ps.executeUpdate();
        }
    }

    private void updateTrackFromRow(Connection conn, TrackRow row) throws SQLException {
        int genreId = getOrCreateGenre(conn, row.getGenre());
        int artistId = getOrCreateArtist(conn, row.getArtist(), genreId);
        Integer albumId = row.getAlbum().isEmpty() ? null : getOrCreateAlbum(conn, row.getAlbum(), artistId);

        String sql = "UPDATE Track SET Title=?, ArtistID=?, AlbumID=?, GenreID=? WHERE TrackID=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, row.getTitle());
            ps.setInt(2, artistId);
            if (albumId == null) ps.setNull(3, Types.INTEGER); else ps.setInt(3, albumId);
            ps.setInt(4, genreId);
            ps.setInt(5, row.getTrackId());
            ps.executeUpdate();
        }
    }

    private int parseDuration(String dur) {
        String[] p = dur.split(":");
        return Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1]);
    }

    @FXML
    private void refreshData() {
        if (isTableMode) {
            loadAllTracksToTable();
        } else {
            loadArtists();
            loadGenres();
        }
    }

    private void resetStatus() {
        statusLabel.setText("");
        statusLabel.setStyle(null);
    }

    private void loadArtists() {
        artistComboBox.getItems().clear();
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

    @FXML
    private void chooseFile() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Аудиофайлы", "*.mp3", "*.wav", "*.flac", "*.ogg", "*.m4a"),
                new FileChooser.ExtensionFilter("Все файлы", "*.*")
        );

        selectedFile = fc.showOpenDialog(getStage());

        if (selectedFile != null) {
            filePathLabel.setText(selectedFile.getName());
            resetStatus();

            try {
                AudioFile audioFile = AudioFileIO.read(selectedFile);
                int duration = audioFile.getAudioHeader().getTrackLength();
                durationLabel.setText(String.format("%d:%02d", duration / 60, duration % 60));

                Tag tag = audioFile.getTagOrCreateAndSetDefault();
                if (tag != null && !tag.isEmpty()) {
                    String titleFromTag = getTagValue(tag, FieldKey.TITLE);
                    if (!titleFromTag.isEmpty() && titleField.getText().isEmpty()) {
                        titleField.setText(titleFromTag);
                    }

                    String artistFromTag = getTagValue(tag, FieldKey.ARTIST);
                    if (!artistFromTag.isEmpty()) {
                        setComboBoxValue(artistComboBox, artistFromTag);
                    } else if (artistComboBox.getValue() == null) {
                        artistComboBox.setValue(null);
                    }

                    String albumFromTag = getTagValue(tag, FieldKey.ALBUM);
                    if (!albumFromTag.isEmpty()) {
                        albumField.setText(albumFromTag);
                    }

                    String genreFromTag = getTagValue(tag, FieldKey.GENRE);
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
            selectedFile = null;
            filePathLabel.setText("Не выбран");
            durationLabel.setText("—");
        }
    }

    private String getTagValue(Tag tag, FieldKey key) {
        String value = tag.getFirst(key);
        return (value != null) ? value.trim() : "";
    }

    private void setComboBoxValue(ComboBox<String> comboBox, String value) {
        if (!comboBox.getItems().contains(value)) {
            comboBox.getItems().add(value);
        }
        comboBox.setValue(value);
    }

    @FXML
    private void addTrack() {
        if (selectedFile == null) {
            showError("Выберите файл!");
            return;
        }

        String title = titleField.getText().trim();
        String artistName = Optional.ofNullable(artistComboBox.getValue()).orElse("").trim();
        String album = albumField.getText().trim();
        String genreName = Optional.ofNullable(genreComboBox.getValue()).orElse("").trim();

        if (title.isEmpty()) {
            title = selectedFile.getName().replaceAll("\\.\\w+$", "");
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
            conn.setAutoCommit(false);

            try {
                int genreId = getOrCreateGenre(conn, genreName);
                int artistId = getOrCreateArtist(conn, artistName, genreId);
                Integer albumId = album.isEmpty() ? null : getOrCreateAlbum(conn, album, artistId);

                Path tracksDir = Path.of(TRACKS_DIR);
                if (!Files.exists(tracksDir)) {
                    Files.createDirectories(tracksDir);
                }

                String fileExtension = getFileExtension(selectedFile);
                String uniqueFileName = System.currentTimeMillis() + "_" + title.replaceAll("[^a-zA-Z0-9_.-]", "_") + fileExtension;
                Path dest = tracksDir.resolve(uniqueFileName);

                if (!selectedFile.toPath().equals(dest)) {
                    Files.copy(selectedFile.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
                } else {
                    throw new IOException("Исходный и конечный файл совпадают. Операция отменена.");
                }

                String dbPath = TRACKS_DIR + "/" + uniqueFileName;
                int durationSeconds = getDurationSeconds(selectedFile);

                String sql = "INSERT INTO Track (Title, ArtistID, AlbumID, Duration, TrackURL, GenreID) VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, title);
                    ps.setInt(2, artistId);

                    if (albumId == null) {
                        ps.setNull(3, java.sql.Types.INTEGER);
                    } else {
                        ps.setInt(3, albumId);
                    }

                    ps.setInt(4, durationSeconds);
                    ps.setString(5, dbPath);
                    ps.setInt(6, genreId);
                    ps.executeUpdate();
                }

                conn.commit();
                showSuccess("Трек успешно добавлен!");
                clearForm();

            } catch (Exception e) {
                conn.rollback();
                showError("Ошибка добавления трека: " + e.getMessage());
                e.printStackTrace();
            }

        } catch (SQLException e) {
            showError("Ошибка подключения к БД: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Исправленный метод для обработки в классическом режиме
    @FXML
    private void addTrackClassic() {
        addTrack();
    }

    private String getFileExtension(File file) {
        String name = file.getName();
        int lastIndexOf = name.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return "";
        }
        return name.substring(lastIndexOf);
    }

    private int getDurationSeconds(File file) throws Exception {
        AudioFile af = AudioFileIO.read(file);
        return af.getAudioHeader().getTrackLength();
    }

    private int getOrCreateArtist(Connection conn, String name, int genreId) throws SQLException {
        if (name == null || name.trim().isEmpty()) {
            name = "Неизвестный исполнитель";
        }
        name = name.trim();

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT ArtistID FROM Artist WHERE LOWER(Name) = LOWER(?)")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int existingId = rs.getInt(1);
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

        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO Artist (Name, GenreID) VALUES (?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setInt(2, genreId);
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

        try (PreparedStatement ps = c.prepareStatement("SELECT AlbumID FROM Album WHERE LOWER(Title) = LOWER(?) AND ArtistID = ?")) {
            ps.setString(1, title);
            ps.setInt(2, artistId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }

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
        return null;
    }

    private int getOrCreateGenre(Connection conn, String name) throws SQLException {
        if (name == null || name.isEmpty()) {
            name = "Unknown";
        }
        name = name.trim();

        try (PreparedStatement ps = conn.prepareStatement("SELECT GenreID FROM Genre WHERE LOWER(Name) = LOWER(?)")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO Genre (Name) VALUES (?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    loadGenres();
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Не удалось создать жанр: " + name);
    }

    private void showSuccess(String message) {
        statusLabel.setStyle("-fx-text-fill: #27ae60;");
        statusLabel.setText(message);
    }

    private void showError(String message) {
        statusLabel.setStyle("-fx-text-fill: #c0392b;");
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
    }

    @FXML
    private void closeWindow() {
        Stage stage = (Stage) titleField.getScene().getWindow();
        if (stage != null) {
            stage.close();
        }
    }

    private Stage getStage() {
        return (Stage) (filePathLabel != null ? filePathLabel.getScene().getWindow() : null);
    }
}