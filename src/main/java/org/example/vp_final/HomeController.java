package org.example.vp_final;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import java.io.File;
import java.nio.file.Paths;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.io.IOException;
import java.nio.file.*;
import java.net.URL;
import java.sql.*;
import java.util.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import java.util.ResourceBundle;
import javafx.util.Duration;

public class HomeController implements Initializable {

    // === Контейнеры ===
    @FXML private FlowPane newTracksContainer;
    @FXML private FlowPane userPlaylistsContainer;
    @FXML private FlowPane afishaContainer;
    @FXML private FlowPane favoriteContentContainer;
    @FXML private FlowPane favoritePlaylistsContainer;
    // === Афиша ===
    @FXML private ChoiceBox<String> afishaSortColumn;
    @FXML private ChoiceBox<String> afishaSortDirection;
    @FXML private Button playButton;
    @FXML private Label currentTrackLabel;
    @FXML private Slider volumeSlider;
    @FXML private Label volumeLabel;
    @FXML private Button playerLikeButton;

    private MediaPlayer mediaPlayer;
    private List<File> trackFiles = new ArrayList<>();
    private int currentTrackIndex = -1;
    private int currentPlayingTrackId = -1;
    private Random random = new Random();

    // --- ГЛАВНОЕ ИЗМЕНЕНИЕ: Маппинг имени файла на TrackID из БД ---
    private Map<String, Integer> fileToTrackIdMap = new HashMap<>();

    // Хранилище данных
    private List<AfishaEvent> afishaEvents = new ArrayList<>();
    private User currentUser;
    private MainController mainController;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
        System.out.println("HomeController: mainController установлен = " + (mainController != null));
    }

    public void setUser(User user) {
        this.currentUser = user;
        // Перезагрузка контента при смене пользователя
        loadUserPlaylists();
        loadAfisha();
        loadFavoriteTracksSection();
        loadFavoritePlaylistsSection();
        updatePlayerLikeButtonState();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadLatestTracks();
        setupAfishaSorting();
        loadTrackFiles(); // --- ОБНОВЛЕН
        setupVolumeControl();
        setupPlayerLikeButton();
    }

    // --- ОБНОВЛЕННЫЙ МЕТОД: Загрузка файлов и маппинга из БД ---
    private void loadTrackFiles() {
        Path tracksDir = Path.of("tracks");
        fileToTrackIdMap.clear();
        trackFiles.clear();

        // 1. Загрузка маппинга TrackURL -> TrackID из БД
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:music_app.db");
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT TrackID, TrackURL FROM Track")) {
            while (rs.next()) {
                int id = rs.getInt("TrackID");
                String url = rs.getString("TrackURL");
                if (url != null && !url.isEmpty()) {
                    // 🔥 ИСПРАВЛЕНИЕ: Извлекаем ТОЛЬКО имя файла из полного пути
                    // "tracks/1764914054484_Lyudvig_van_Betkhoven_-_Lunnaya_sonata_48113982.mp3"
                    // -> "1764914054484_Lyudvig_van_Betkhoven_-_Lunnaya_sonata_48113982.mp3"
                    String fileName = Paths.get(url).getFileName().toString();
                    fileToTrackIdMap.put(fileName, id);

                    System.out.println("DEBUG: Маппинг загружен - '" + fileName + "' -> ID: " + id);
                }
            }
            System.out.println("ОТЛАДКА: Загружен маппинг " + fileToTrackIdMap.size() + " треков из БД.");
        } catch (SQLException e) {
            System.err.println("Ошибка загрузки URL треков из БД: " + e.getMessage());
        }

        // 2. Загрузка реальных файлов из папки
        if (Files.exists(tracksDir) && Files.isDirectory(tracksDir)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(tracksDir, "*.{mp3,wav,flac,ogg,m4a}")) {
                for (Path path : stream) {
                    trackFiles.add(path.toFile());
                    System.out.println("DEBUG: Файл найден - " + path.getFileName());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (trackFiles.isEmpty()) {
            currentTrackLabel.setText("Нет треков в папке tracks/");
            playButton.setDisable(true);
        } else {
            playButton.setDisable(false);
            stopCurrentTrackAndReset();
        }
    }

    private void setupVolumeControl() {
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double volume = newVal.doubleValue() / 100.0;
            if (mediaPlayer != null) {
                mediaPlayer.setVolume(volume);
            }
            volumeLabel.setText(String.format("%.0f%%", newVal.doubleValue()));
        });
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(volumeSlider.getValue() / 100.0);
        }
        volumeLabel.setText(String.format("%.0f%%", volumeSlider.getValue()));
    }

    // --- МЕТОД: Настройка кнопки лайка плеера (без изменений) ---
    private void setupPlayerLikeButton() {
        if (playerLikeButton != null) {
            playerLikeButton.setStyle("-fx-background-color: transparent;");
            playerLikeButton.setPrefSize(32, 32);
            updatePlayerLikeButtonState();

            playerLikeButton.setOnAction(e -> {
                if (currentUser == null) {
                    showAlert("Войдите в аккаунт, чтобы добавлять в избранное!");
                    return;
                }
                if (currentPlayingTrackId == -1) {
                    showAlert("Сначала начните воспроизведение трека.");
                    return;
                }

                try {
                    if (DatabaseHelper.isTrackLiked(currentUser.userId(), currentPlayingTrackId)) {
                        DatabaseHelper.removeFromFavorites(currentUser.userId(), currentPlayingTrackId);
                    } else {
                        DatabaseHelper.addToFavorites(currentUser.userId(), currentPlayingTrackId);
                    }
                    updatePlayerLikeButtonState();
                    loadFavoriteTracksSection();
                } catch (SQLException ex) {
                    showAlert("Ошибка при работе с избранным: " + ex.getMessage());
                }
            });
        }
    }

    // --- МЕТОД: Обновление состояния кнопки лайка плеера (без изменений) ---
    private void updatePlayerLikeButtonState() {
        if (playerLikeButton == null || currentUser == null) {
            if (playerLikeButton != null) {
                updateLikeButton(playerLikeButton, false);
                playerLikeButton.setDisable(true);
            }
            return;
        }

        boolean isLiked = false;
        boolean isPlayable = currentPlayingTrackId != -1;

        if (isPlayable) {
            isLiked = isTrackLiked(currentPlayingTrackId);
        }

        updateLikeButton(playerLikeButton, isLiked);
        playerLikeButton.setDisable(!isPlayable);
    }


    // --- НОВЫЙ/ОБНОВЛЕННЫЙ МЕТОД: Получение ID для файла (ПРИОРИТЕТ - БД) ---
    private int getTrackIdForFile(File file) {
        String fileName = file.getName();

        // 1. ПРИОРИТЕТ: Поиск ID по имени файла в мапе (используя TrackURL из БД)
        if (fileToTrackIdMap.containsKey(fileName)) {
            int dbTrackId = fileToTrackIdMap.get(fileName);
            System.out.println("--- ПЛЕЕР --- Трек ID для лайка: " + dbTrackId + " (Источник ID: База Данных/TrackURL)");
            return dbTrackId;
        }

        // 2. ЗАПАСНОЙ ВАРИАНТ: Парсинг имени файла (если нет в БД)
        int trackId = -1;
        try {
            int idEnd = fileName.indexOf('_');

            if (idEnd > 0) {
                String idString = fileName.substring(0, idEnd);
                trackId = Integer.parseInt(idString);
            }
        } catch (NumberFormatException e) {
            // Игнорируем ошибку парсинга
        }

        if (trackId != -1) {
            System.out.println("--- ПЛЕЕР --- Трек ID для лайка: " + trackId + " (Источник ID: Парсинг Имени/Запасной вариант)");
        } else {
            System.out.println("--- ПЛЕЕР --- Трек ID для лайка: -1 (Источник ID: Ошибка Файла - не найден в БД и не соответствует формату)");
        }

        return trackId;
    }

    @FXML
    private void playRandomTrack() {
        if (trackFiles.isEmpty()) return;

        if (mediaPlayer != null && mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            stopCurrentTrackAndReset();
            return;
        }

        stopCurrentTrack();
        currentTrackIndex = random.nextInt(trackFiles.size());
        File trackFile = trackFiles.get(currentTrackIndex);

        int trackId = getTrackIdForFile(trackFile); // <--- Используем новый метод

        playTrack(trackFile, trackId);
        playButton.setText("Стоп");
        playButton.setOnAction(e -> stopCurrentTrackAndReset());
    }

    private void playTrack(File file, int trackId) {
        stopCurrentTrack();

        currentPlayingTrackId = trackId;

        // Логирование теперь происходит внутри getTrackIdForFile() и playTrackById()
        if (trackId == -1) {
            System.out.println("--- ПЛЕЕР --- Трек ID для лайка: -1 (Лайк недоступен)");
        }

        currentTrackLabel.setText("Играет: " + file.getName().replaceAll("^\\d+_", "").replace("_", " "));

        Media media = new Media(file.toURI().toString());
        mediaPlayer = new MediaPlayer(media);
        mediaPlayer.setVolume(volumeSlider.getValue() / 100.0);

        mediaPlayer.setOnEndOfMedia(this::playNext);
        mediaPlayer.play();

        updatePlayerLikeButtonState();
    }

    // Этот метод теперь используется только как вспомогательный,
    // он будет использовать логику getTrackIdForFile, если вызывается
    private void playTrack(File file) {
        int trackId = getTrackIdForFile(file);
        playTrack(file, trackId);
    }

    private void stopCurrentTrack() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
    }

    private void stopCurrentTrackAndReset() {
        stopCurrentTrack();

        currentPlayingTrackId = -1;

        currentTrackLabel.setText("Нажмите ЗАПУСТИТЬ");
        playButton.setText("ЗАПУСТИТЬ");
        playButton.setOnAction(e -> playRandomTrack());

        updatePlayerLikeButtonState();
    }

    @FXML
    private void playPrevious() {
        if (trackFiles.isEmpty()) return;
        stopCurrentTrack();

        if (currentTrackIndex == -1) {
            currentTrackIndex = trackFiles.size() - 1;
        } else {
            currentTrackIndex = (currentTrackIndex - 1 + trackFiles.size()) % trackFiles.size();
        }

        File trackFile = trackFiles.get(currentTrackIndex);
        playTrack(trackFile, getTrackIdForFile(trackFile)); // <--- Используем новый метод
    }

    @FXML
    private void playNext() {
        if (trackFiles.isEmpty()) return;
        stopCurrentTrack();

        if (currentTrackIndex == -1) {
            currentTrackIndex = 0;
        } else {
            currentTrackIndex = (currentTrackIndex + 1) % trackFiles.size();
        }

        File trackFile = trackFiles.get(currentTrackIndex);
        playTrack(trackFile, getTrackIdForFile(trackFile)); // <--- Используем новый метод
    }

    // --- ОБНОВЛЕННЫЙ МЕТОД: playTrackById ---
    // --- ИСПРАВЛЕННЫЙ МЕТОД: playTrackById ---
    private void playTrackById(int trackId) {
        String trackURL = null;

        // 1. Находим TrackURL по TrackID из БД
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:music_app.db");
             PreparedStatement pstmt = conn.prepareStatement("SELECT TrackURL FROM Track WHERE TrackID = ?")) {
            pstmt.setInt(1, trackId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                trackURL = rs.getString("TrackURL");
            }
        } catch (SQLException e) {
            System.err.println("Ошибка поиска TrackURL для ID:" + trackId + " в БД.");
        }

        File fileToPlay = null;

        // 2. Ищем файл по TrackURL (предполагаем, что TrackURL == file.getName())
        if (trackURL != null) {
            // 🔥 ИСПРАВЛЕНИЕ: Создаем final или effectively final копию переменной.
            final String finalTrackURL = trackURL;

            fileToPlay = trackFiles.stream()
                    .filter(f -> f.getName().equals(finalTrackURL)) // Используем finalTrackURL
                    .findFirst()
                    .orElse(null);
        }

        // 3. Запасной вариант (если файл не найден по точному URL, ищем по старому формату ID_)
        if (fileToPlay == null) {
            fileToPlay = trackFiles.stream()
                    .filter(f -> f.getName().startsWith(trackId + "_"))
                    .findFirst()
                    .orElse(null);
        }

        if (fileToPlay != null) {
            currentTrackIndex = trackFiles.indexOf(fileToPlay);
            System.out.println("--- ПЛЕЕР --- Трек ID для лайка: " + trackId + " (Источник ID: Карточка/База Данных)");
            // Используем TrackID, полученный из карточки, который точно корректен
            playTrack(fileToPlay, trackId);
        } else {
            showAlert("Ошибка: Файл трека (ID:" + trackId + ") не найден в папке tracks/. Проверьте TrackURL в БД.");
        }
    }

    private void loadLatestTracks() {
        newTracksContainer.getChildren().clear();
        String sql = """
            SELECT t.TrackID, t.Title, a.Name AS ArtistName
            FROM Track t
            JOIN Artist a ON t.ArtistID = a.ArtistID
            ORDER BY t.TrackID DESC
            LIMIT 6
            """;

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:music_app.db");
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            boolean hasTracks = false;
            while (rs.next()) {
                hasTracks = true;
                int trackId = rs.getInt("TrackID");
                String title = rs.getString("Title");
                String artist = rs.getString("ArtistName");
                if (artist == null) artist = "Неизвестный исполнитель";

                VBox card = createTrackCard(trackId, title, artist);
                newTracksContainer.getChildren().add(card);
            }
            if (!hasTracks) {
                showPlaceholder(newTracksContainer, "В разделе 'Новое' пока нет треков.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showPlaceholder(newTracksContainer, "Ошибка загрузки новых треков.");
        }
    }

    private void setupAfishaSorting() {
        afishaSortColumn.getItems().addAll("Название", "Дата", "Место");
        afishaSortColumn.setValue("Дата");

        afishaSortDirection.getItems().addAll("↑ Возрастание", "↓ Убывание");
        afishaSortDirection.setValue("↑ Возрастание");

        afishaSortColumn.valueProperty().addListener((obs, oldVal, newVal) -> sortAndDisplayAfisha());
        afishaSortDirection.valueProperty().addListener((obs, oldVal, newVal) -> sortAndDisplayAfisha());
    }

    private void showPlaceholder(FlowPane container, String text) {
        container.getChildren().clear();
        Label placeholder = new Label(text);
        placeholder.setStyle("-fx-font-size: 18; -fx-text-fill: #95a5a6; -fx-padding: 40 0 0 0;");
        placeholder.setWrapText(true);
        container.getChildren().add(placeholder);
    }

    private void loadAfisha() {
        if (currentUser == null) {
            showPlaceholder(afishaContainer, "Войдите в систему, чтобы увидеть Афишу.");
            return;
        }
        afishaEvents.clear();

        String sql = "SELECT AfishaID, Title, Date, Location FROM Afisha ORDER BY Date DESC LIMIT 10";

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:music_app.db");
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                afishaEvents.add(new AfishaEvent(
                        rs.getInt("AfishaID"),
                        rs.getString("Title"),
                        rs.getString("Date"),
                        rs.getString("Location")
                ));
            }
            sortAndDisplayAfisha();
        } catch (SQLException e) {
            e.printStackTrace();
            showPlaceholder(afishaContainer, "Ошибка загрузки афиши.");
        }
    }

    private void sortAndDisplayAfisha() {
        if (afishaEvents.isEmpty()) {
            showPlaceholder(afishaContainer, "Предстоящих событий нет.");
            return;
        }

        String column = afishaSortColumn.getValue();
        boolean ascending = "↑ Возрастание".equals(afishaSortDirection.getValue());

        Comparator<AfishaEvent> comparator = switch (column) {
            case "Название" -> Comparator.comparing(AfishaEvent::title);
            case "Место" -> Comparator.comparing(AfishaEvent::location);
            case "Дата" -> Comparator.comparing(AfishaEvent::date);
            default -> Comparator.comparing(AfishaEvent::afishaId);
        };

        if (!ascending) comparator = comparator.reversed();
        quicksort(afishaEvents, comparator);
        displayAfisha();
    }

    private void displayAfisha() {
        afishaContainer.getChildren().clear();
        for (AfishaEvent event : afishaEvents) {
            afishaContainer.getChildren().add(createAfishaCard(event));
        }
    }

    private VBox createAfishaCard(AfishaEvent event) {
        VBox card = new VBox(8);
        card.setPrefSize(180, 180);
        card.setStyle("-fx-background-color: #ecf0f1; -fx-padding: 15; -fx-background-radius: 12; -fx-cursor: hand;");
        Label title = new Label(event.title());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 15; -fx-text-fill: #2c3e50;");
        Label date = new Label("Когда: " + event.date());
        date.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 13;");
        Label loc = new Label("Где: " + event.location());
        loc.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 13;");

        title.setWrapText(true);
        loc.setWrapText(true);

        card.getChildren().addAll(title, date, loc);
        return card;
    }

    private boolean isTrackLiked(int trackId) {
        if (currentUser == null) return false;
        try {
            return DatabaseHelper.isTrackLiked(currentUser.userId(), trackId); //
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void updateLikeButton(Button likeButton, boolean isLiked) {
        String iconPath = isLiked
                ? "/org/example/vp_final/icons/heart-filled.png"
                : "/org/example/vp_final/icons/heart-empty.png";
        try {
            Image image = new Image(getClass().getResourceAsStream(iconPath));
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(24);
            imageView.setFitHeight(24);
            likeButton.setGraphic(imageView);
            likeButton.setText(null);
        } catch (Exception e) {
            System.err.println("Ошибка загрузки иконки: " + iconPath + ". Используется текстовый запасной вариант.");
            likeButton.setGraphic(null);
            likeButton.setText(isLiked ? "♥" : "♡");
            likeButton.setStyle("-fx-background-color: transparent; -fx-text-fill: "
                    + (isLiked ? "red" : "white")
                    + "; -fx-font-size: 18px;");
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Информация");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void loadFavoriteTracksSection() {
        if (currentUser == null || favoriteContentContainer == null) {
            if (favoriteContentContainer != null) {
                showPlaceholder(favoriteContentContainer, "Войдите, чтобы увидеть избранные треки.");
            }
            return;
        }

        favoriteContentContainer.getChildren().clear();

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

            boolean has = false;
            while (rs.next()) {
                has = true;
                VBox card = createTrackCard(
                        rs.getInt("TrackID"),
                        rs.getString("Title"),
                        rs.getString("ArtistName") != null ? rs.getString("ArtistName") : "Неизвестный исполнитель"
                );
                favoriteContentContainer.getChildren().add(card);
            }

            if (!has) {
                showPlaceholder(favoriteContentContainer, "Нет любимых треков");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showPlaceholder(favoriteContentContainer, "Ошибка загрузки треков");
        }
    }

    private void loadFavoritePlaylistsSection() {
        if (currentUser == null || favoritePlaylistsContainer == null) {
            if (favoritePlaylistsContainer != null) {
                showPlaceholder(favoritePlaylistsContainer, "Войдите, чтобы увидеть избранные плейлисты.");
            }
            return;
        }

        favoritePlaylistsContainer.getChildren().clear();

        String sql = """
        SELECT p.PlaylistID, p.Title, p.CreationDate, u.Username AS OwnerName
        FROM Playlist p
        JOIN UserLikePlaylist ulp ON p.PlaylistID = ulp.PlaylistID
        JOIN User u ON p.UserID = u.UserID
        WHERE ulp.UserID = ?
        ORDER BY p.CreationDate DESC
        """;

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:music_app.db");
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, currentUser.userId());
            ResultSet rs = pstmt.executeQuery();

            boolean has = false;
            while (rs.next()) {
                has = true;
                String title = rs.getString("Title");
                String owner = rs.getString("OwnerName");
                String date = rs.getString("CreationDate");

                VBox card = createFavoritePlaylistCard(title, owner, date);
                favoritePlaylistsContainer.getChildren().add(card);
            }

            if (!has) {
                showPlaceholder(favoritePlaylistsContainer, "Нет любимых плейлистов");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showPlaceholder(favoritePlaylistsContainer, "Ошибка загрузки любимых плейлистов");
        }
    }

    private VBox createFavoritePlaylistCard(String title, String owner, String date) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(18));
        card.setMinWidth(220);
        card.setStyle("-fx-background-color: #e74c3c; -fx-background-radius: 18; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 12, 0.3, 0, 4); -fx-cursor: hand;");

        Label t = new Label(title);
        t.setStyle("-fx-font-weight: bold; -fx-font-size: 17; -fx-text-fill: white;");
        t.setWrapText(true);

        Label o = new Label("от " + (owner != null ? owner : "Аноним"));
        o.setStyle("-fx-text-fill: #fadbd8; -fx-font-size: 13;");

        Label d = new Label(date != null && date.length() >= 10 ? date.substring(0, 10) : "");
        d.setStyle("-fx-text-fill: #fadbd8; -fx-font-size: 11;");

        card.getChildren().addAll(t, o, d);
        card.setOnMouseClicked(e -> System.out.println("Открыть плейлист: " + title));
        return card;
    }

    private VBox createTrackCard(int trackId, String title, String artist) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 14; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 8, 0, 0, 2); -fx-cursor: hand;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 15; -fx-text-fill: #2c3e50;");

        Label artistLabel = new Label(artist);
        artistLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 13;");

        Button likeButton = new Button();
        likeButton.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
        likeButton.setPrefSize(32, 32);

        boolean initialLikedState = currentUser != null && isTrackLiked(trackId);
        updateLikeButton(likeButton, initialLikedState);

        likeButton.setOnAction(e -> {
            if (currentUser == null) {
                showAlert("Войдите в аккаунт, чтобы добавлять в избранное!");
                return;
            }

            boolean isCurrentlyLiked = isTrackLiked(trackId);

            try {
                if (isCurrentlyLiked) {
                    DatabaseHelper.removeFromFavorites(currentUser.userId(), trackId); //
                } else {
                    DatabaseHelper.addToFavorites(currentUser.userId(), trackId); //
                }
            } catch (SQLException ex) {
                showAlert("Ошибка при работе с избранным: " + ex.getMessage());
            }

            updateLikeButton(likeButton, !isCurrentlyLiked);
            loadFavoriteTracksSection();
            if (currentPlayingTrackId == trackId) {
                updatePlayerLikeButtonState();
            }

            e.consume();
        });

        card.setOnMouseClicked(e -> {
            // Проверяем, что цель клика не является likeButton или его дочерним элементом
            if (e.getTarget() instanceof Button || e.getTarget() instanceof ImageView) {
                return;
            }
            playTrackById(trackId);
        });

        HBox bottom = new HBox(10, artistLabel, new Region(), likeButton);
        bottom.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(bottom.getChildren().get(1), Priority.ALWAYS);

        card.getChildren().addAll(titleLabel, bottom);
        return card;
    }

    private void loadUserPlaylists() {
        if (currentUser == null || userPlaylistsContainer == null) {
            if (userPlaylistsContainer != null) {
                showPlaceholder(userPlaylistsContainer, "Войдите, чтобы увидеть свои плейлисты.");
            }
            return;
        }
        userPlaylistsContainer.getChildren().clear();

        String sql = "SELECT PlaylistID, Title, CreationDate FROM Playlist WHERE UserID = ? ORDER BY CreationDate DESC";

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:music_app.db");
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, currentUser.userId());
            ResultSet rs = pstmt.executeQuery();

            boolean has = false;
            while (rs.next()) {
                has = true;
                VBox card = createPlaylistCard(rs.getString("Title"), rs.getString("CreationDate"));
                userPlaylistsContainer.getChildren().add(card);
            }
            if (!has) {
                Label l = new Label("У вас пока нет своих плейлистов");
                l.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 15; -fx-padding: 20;");
                userPlaylistsContainer.getChildren().add(l);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showPlaceholder(userPlaylistsContainer, "Ошибка загрузки ваших плейлистов.");
        }
    }

    private VBox createPlaylistCard(String title, String date) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 14; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 8, 0, 0, 2); -fx-cursor: hand;");

        Label t = new Label(title);
        t.setStyle("-fx-font-weight: bold; -fx-font-size: 15; -fx-text-fill: #2c3e50;");
        t.setWrapText(true);

        Label d = new Label("Создан: " + (date != null && date.length() >= 10 ? date.substring(0, 10) : "Недавно"));
        d.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12;");

        card.getChildren().addAll(t, d);
        return card;
    }

    // Вспомогательные методы для сортировки (оставлены без изменений)
    private void quicksort(List<AfishaEvent> list, Comparator<AfishaEvent> comparator) {
        quicksort(list, 0, list.size() - 1, comparator);
    }

    private void quicksort(List<AfishaEvent> list, int low, int high, Comparator<AfishaEvent> comparator) {
        if (low < high) {
            if (high - low < 10) {
                insertionSort(list, low, high, comparator);
                return;
            }
            int pi = partition(list, low, high, comparator);
            quicksort(list, low, pi - 1, comparator);
            quicksort(list, pi + 1, high, comparator);
        }
    }

    private int partition(List<AfishaEvent> list, int low, int high, Comparator<AfishaEvent> comparator) {
        int mid = medianOfThree(list, low, high, comparator);
        swap(list, mid, high);
        AfishaEvent pivot = list.get(high);
        int i = low - 1;
        for (int j = low; j < high; j++) {
            if (comparator.compare(list.get(j), pivot) <= 0) {
                i++;
                swap(list, i, j);
            }
        }
        swap(list, i + 1, high);
        return i + 1;
    }

    private int medianOfThree(List<AfishaEvent> list, int low, int high, Comparator<AfishaEvent> comparator) {
        int center = low + (high - low) / 2;
        if (comparator.compare(list.get(low), list.get(center)) > 0) swap(list, low, center);
        if (comparator.compare(list.get(low), list.get(high)) > 0) swap(list, low, high);
        if (comparator.compare(list.get(center), list.get(high)) > 0) swap(list, center, high);
        return center;
    }

    private void swap(List<AfishaEvent> list, int i, int j) {
        AfishaEvent temp = list.get(i);
        list.set(i, list.get(j));
        list.set(j, temp);
    }

    private void insertionSort(List<AfishaEvent> list, int low, int high, Comparator<AfishaEvent> comparator) {
        for (int i = low + 1; i <= high; i++) {
            AfishaEvent key = list.get(i);
            int j = i - 1;
            while (j >= low && comparator.compare(list.get(j), key) > 0) {
                list.set(j + 1, list.get(j));
                j--;
            }
            list.set(j + 1, key);
        }
    }

    @FXML private void onMainAction() {
        System.out.println("Запуск основного действия");
    }
    @FXML
    public void openSearch() {
        if (mainController != null) {
            mainController.showSearch();
        } else {
            System.out.println("ОШИБКА: mainController == null → ничего не произойдёт");
        }
    }

    @FXML
    public void openProfile() {

        if (mainController != null) {
            mainController.showProfile();
        } else {
            System.out.println("ОШИБКА: mainController == null → ничего не произойдёт");
        }
    }

}