package org.example.vp_final;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import java.io.File;
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

    private MediaPlayer mediaPlayer;
    private List<File> trackFiles = new ArrayList<>();
    private int currentTrackIndex = -1;
    private Random random = new Random();

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
        // ВАЖНО: нужно вызвать эти методы после установки currentUser
        loadFavoriteTracksSection();
        loadFavoritePlaylistsSection();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadLatestTracks();
        setupAfishaSorting();
        loadTrackFiles();
        setupVolumeControl();

        // УДАЛЕНО: Вызов здесь, потому что он должен выполняться только после установки currentUser в setUser.
        // Если пользователь не вошел, эти контейнеры будут пустыми, что нормально.
        // loadFavoriteTracksSection();
        // loadFavoritePlaylistsSection();
    }

    private void loadTrackFiles() {
        Path tracksDir = Path.of("tracks");
        if (Files.exists(tracksDir) && Files.isDirectory(tracksDir)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(tracksDir, "*.{mp3,wav,flac,ogg,m4a}")) {
                for (Path path : stream) {
                    trackFiles.add(path.toFile());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (trackFiles.isEmpty()) {
            currentTrackLabel.setText("Нет треков в папке tracks/");
            playButton.setDisable(true);
        } else {
            // ИСПРАВЛЕНИЕ: если треки есть, кнопка должна быть активна
            playButton.setDisable(false);
            stopCurrentTrackAndReset(); // Устанавливаем начальный текст и действие кнопки
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
        // ИСПРАВЛЕНИЕ: Устанавливаем начальное значение громкости для mediaPlayer, если он уже существует
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(volumeSlider.getValue() / 100.0);
        }
        volumeLabel.setText(String.format("%.0f%%", volumeSlider.getValue()));
    }

    @FXML
    private void playRandomTrack() {
        if (trackFiles.isEmpty()) return;

        // ИСПРАВЛЕНИЕ: Проверяем, не играет ли уже что-то. Если да, останавливаем.
        if (mediaPlayer != null && mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            stopCurrentTrackAndReset();
            return;
        }

        stopCurrentTrack();
        currentTrackIndex = random.nextInt(trackFiles.size());
        playTrack(trackFiles.get(currentTrackIndex));
        playButton.setText("Стоп");
        // ИСПРАВЛЕНИЕ: При нажатии "Стоп" нужно остановить и сбросить состояние
        playButton.setOnAction(e -> stopCurrentTrackAndReset());
    }

    private void playTrack(File file) {
        // ИСПРАВЛЕНИЕ: Если трек уже играет (например, при вызове playNext/playPrevious), сначала останавливаем.
        stopCurrentTrack();

        currentTrackLabel.setText("Играет: " + file.getName().replaceAll("^\\d+_", "").replace("_", " "));

        Media media = new Media(file.toURI().toString());
        mediaPlayer = new MediaPlayer(media);
        mediaPlayer.setVolume(volumeSlider.getValue() / 100.0);

        mediaPlayer.setOnEndOfMedia(this::playNext);
        mediaPlayer.play();

        // Убедимся, что кнопка переключена на "Стоп"
        playButton.setText("Стоп");
        playButton.setOnAction(e -> stopCurrentTrackAndReset());
    }

    private void stopCurrentTrack() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
            // ИСПРАВЛЕНИЕ: Не сбрасываем здесь текст и действие, так как этот метод
            // используется как вспомогательный для перехода к следующему треку.
        }
    }

    private void stopCurrentTrackAndReset() {
        stopCurrentTrack();
        currentTrackLabel.setText("Нажмите ЗАПУСТИТЬ");
        playButton.setText("ЗАПУСТИТЬ");
        playButton.setOnAction(e -> playRandomTrack());
        currentTrackIndex = -1; // Сброс индекса текущего трека
    }

    @FXML
    private void playPrevious() {
        if (trackFiles.isEmpty()) return;
        // ИСПРАВЛЕНИЕ: Если ничего не играло, начинаем с последнего трека
        if (currentTrackIndex == -1) {
            currentTrackIndex = trackFiles.size() - 1;
        } else {
            currentTrackIndex = (currentTrackIndex - 1 + trackFiles.size()) % trackFiles.size();
        }
        playTrack(trackFiles.get(currentTrackIndex));
    }

    @FXML
    private void playNext() {
        if (trackFiles.isEmpty()) return;
        // ИСПРАВЛЕНИЕ: Если ничего не играло, начинаем с первого трека
        if (currentTrackIndex == -1) {
            currentTrackIndex = 0;
        } else {
            currentTrackIndex = (currentTrackIndex + 1) % trackFiles.size();
        }
        playTrack(trackFiles.get(currentTrackIndex));
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
        // ИСПРАВЛЕНИЕ: Сначала очищаем контейнер
        container.getChildren().clear();
        Label placeholder = new Label(text);
        placeholder.setStyle("-fx-font-size: 18; -fx-text-fill: #95a5a6; -fx-padding: 40 0 0 0;");
        placeholder.setWrapText(true);
        // ИСПРАВЛЕНИЕ: Добавляем плейсхолдер
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
            // ИСПРАВЛЕНИЕ: Для корректной сортировки по дате нужно использовать более сложный компаратор,
            // если поле `date` — это строка, или изменить его на `java.sql.Date`/`java.time.LocalDate`.
            // Для простоты оставим сравнение строк, но отметим это как потенциальное место ошибки.
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

        title.setWrapText(true); // + ДОБАВЛЕНО: для длинных названий
        loc.setWrapText(true);   // + ДОБАВЛЕНО: для длинных локаций

        card.getChildren().addAll(title, date, loc);
        return card;
    }

    // Вспомогательные методы для лайков/избранного (отсутствовали, добавлены)

    private boolean isTrackLiked(int trackId) {
        if (currentUser == null) return false;
        String sql = "SELECT COUNT(*) FROM UserLike WHERE UserID = ? AND TrackID = ?";
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:music_app.db");
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, currentUser.userId());
            pstmt.setInt(2, trackId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void addToFavorites(int trackId) {
        String sql = "INSERT INTO UserLike (UserID, TrackID) VALUES (?, ?)";
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:music_app.db");
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, currentUser.userId());
            pstmt.setInt(2, trackId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            // ВАЖНО: Может произойти ошибка, если запись уже существует (нарушение UNIQUE-констрейнта)
            // ИСПРАВЛЕНИЕ: Отображение ошибки пользователю
            showAlert("Ошибка: Не удалось добавить трек в избранное.");
        }
    }

    private void removeFromFavorites(int trackId) {
        String sql = "DELETE FROM UserLike WHERE UserID = ? AND TrackID = ?";
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:music_app.db");
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, currentUser.userId());
            pstmt.setInt(2, trackId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Ошибка: Не удалось удалить трек из избранного.");
        }
    }

    private void updateLikeButton(Button likeButton, boolean isLiked) {
        String iconPath = isLiked
                ? "/org/example/vp_final/icons/heart-filled.png"
                : "/org/example/vp_final/icons/heart-empty.png";
        try {
            Image image = new Image(getClass().getResourceAsStream(iconPath));
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(24); // Установим размер иконки
            imageView.setFitHeight(24);
            likeButton.setGraphic(imageView);
        } catch (NullPointerException e) {
            System.err.println("Ошибка загрузки иконки: " + iconPath);
            likeButton.setText(isLiked ? "♥" : "♡"); // Запасной вариант
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Информация");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void playTrackById(int trackId) {
        // ИСПРАВЛЕНИЕ: Находим файл трека по TrackID в базе, а затем в файловой системе.
        // Для простоты в этом примере, если `trackFiles` уже загружен, можем найти его там,
        // но это требует маппинга TrackID <-> File.name.
        // Предполагаем, что имя файла содержит ID, например "123_TrackName.mp3".

        // Если `trackFiles` не имеет маппинга, нужно найти путь к файлу по TrackID в БД.
        String trackFileName = getTrackFileNameById(trackId);
        if (trackFileName == null) {
            showAlert("Ошибка: Не удалось найти файл трека.");
            return;
        }

        File fileToPlay = trackFiles.stream()
                .filter(f -> f.getName().contains(trackFileName)) // Упрощенный поиск
                .findFirst()
                .orElse(null);

        if (fileToPlay != null) {
            currentTrackIndex = trackFiles.indexOf(fileToPlay);
            playTrack(fileToPlay);
        } else {
            showAlert("Ошибка: Файл трека не найден в папке tracks/.");
        }
    }

    private String getTrackFileNameById(int trackId) {
        // ВАЖНО: Этот метод предполагает, что в таблице Track есть колонка `FileName`
        // или что имя файла может быть восстановлено по Title или ID.
        // Если база данных не содержит имени файла, это место, где возникнет проблема.
        // Для этого исправления предполагаем, что `Title` достаточно, или нужно добавить поле в БД.
        // Если имя файла — это, например, `TrackID_Title.mp3`:
        String sql = "SELECT Title FROM Track WHERE TrackID = ?";
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:music_app.db");
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, trackId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String title = rs.getString("Title");
                // Упрощенное предположение: ищем файл, содержащий ID и название.
                return String.valueOf(trackId); // Ищем файл, содержащий ID
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }


    private void loadFavoriteTracksSection() {
        if (currentUser == null || favoriteContentContainer == null) {
            // ИСПРАВЛЕНИЕ: Если пользователя нет, показываем заглушку
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
                // ИСПРАВЛЕНИЕ: Важно передать правильные данные
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

    // УДАЛЕНО: Дублирующий метод loadFavoriteTracks() был удален,
    // так как loadFavoriteTracksSection() выполняет ту же логику.

    private void loadFavoritePlaylistsSection() {
        if (currentUser == null || favoritePlaylistsContainer == null) {
            // ИСПРАВЛЕНИЕ: Если пользователя нет, показываем заглушку
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

        Label d = new Label(date != null && date.length() >= 10 ? date.substring(0, 10) : ""); // + ИСПРАВЛЕНИЕ: Проверка длины строки
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
        titleLabel.setWrapText(true); // + ДОБАВЛЕНО: Для длинных названий треков

        Label artistLabel = new Label(artist);
        artistLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 13;");

        // Кнопка "В избранное"
        Button likeButton = new Button();
        likeButton.setStyle("-fx-background-color: transparent;");
        // ИСПРАВЛЕНИЕ: Убираем изначальную загрузку иконки, она будет в updateLikeButton
        // likeButton.setGraphic(new ImageView(new Image(
        //         getClass().getResourceAsStream("/org/example/vp_final/icons/heart-empty.png")
        // )));
        likeButton.setPrefSize(32, 32);

        // Проверяем, уже в избранном ли трек
        boolean isLiked = isTrackLiked(trackId);
        // ИСПРАВЛЕНИЕ: Присваиваем boolean переменной, чтобы использовать ее в обработчике
        final boolean[] isLikedRef = new boolean[]{isLiked};

        updateLikeButton(likeButton, isLikedRef[0]);

        // Обработчик клика
        likeButton.setOnAction(e -> {
            if (currentUser == null) {
                showAlert("Войдите в аккаунт, чтобы добавлять в избранное!");
                return;
            }
            if (isLikedRef[0]) {
                removeFromFavorites(trackId);
            } else {
                addToFavorites(trackId);
            }
            isLikedRef[0] = !isLikedRef[0];
            updateLikeButton(likeButton, isLikedRef[0]);
            // Обновляем раздел "Любимое"
            loadFavoriteTracksSection();
        });

        // Клик по карточке — воспроизведение
        card.setOnMouseClicked(e -> {
            // ИСПРАВЛЕНИЕ: Проверяем, что цель клика не является likeButton
            if (e.getTarget() instanceof Button || e.getTarget() instanceof ImageView) {
                // Если клик был по кнопке или иконке внутри кнопки, игнорируем
                return;
            }
            // Дополнительная проверка, чтобы убедиться, что мы не кликнули на элементы внутри HBox,
            // если они не были самой карточкой, но это сложнее. Лучше использовать `consume()`.

            if (e.getTarget() != likeButton && !likeButton.getChildrenUnmodifiable().contains(e.getTarget())) {
                playTrackById(trackId);
                e.consume(); // Предотвращаем дальнейшее всплытие события
            }
        });

        HBox bottom = new HBox(10, artistLabel, new Region(), likeButton);
        bottom.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(bottom.getChildren().get(1), Priority.ALWAYS);

        card.getChildren().addAll(titleLabel, bottom);
        return card;
    }

    private void loadUserPlaylists() {
        if (currentUser == null || userPlaylistsContainer == null) {
            // ИСПРАВЛЕНИЕ: Если пользователя нет, показываем заглушку
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
        t.setWrapText(true); // + ДОБАВЛЕНО: Для длинных названий плейлистов

        Label d = new Label("Создан: " + (date != null && date.length() >= 10 ? date.substring(0, 10) : "Недавно")); // + ИСПРАВЛЕНИЕ: Проверка длины
        d.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12;");

        card.getChildren().addAll(t, d);
        return card;
    }

    // Quicksort и вспомогательные методы (оставлены без изменений)
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

    // ВАЖНО: Требуется класс User и AfishaEvent, которых нет в файле.
    // Если они отсутствуют, код не скомпилируется.
}