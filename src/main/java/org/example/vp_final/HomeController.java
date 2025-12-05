package org.example.vp_final;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import java.io.File;
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
        loadUserPlaylists();
        loadAfisha();

        loadFavoriteTracksSection();
        loadFavoritePlaylistsSection();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadLatestTracks();
        setupAfishaSorting();
        loadTrackFiles();
        setupVolumeControl();

        // Загружаем контент при первом открытии экрана
        loadFavoriteTracksSection();
        loadFavoritePlaylistsSection();
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
    }

    @FXML
    private void playRandomTrack() {
        if (trackFiles.isEmpty()) return;

        stopCurrentTrack();
        currentTrackIndex = random.nextInt(trackFiles.size());
        playTrack(trackFiles.get(currentTrackIndex));
        playButton.setText("Стоп");
        playButton.setOnAction(e -> stopCurrentTrackAndReset());
    }

    private void playTrack(File file) {
        currentTrackLabel.setText("Играет: " + file.getName().replaceAll("^\\d+_", "").replace("_", " "));

        Media media = new Media(file.toURI().toString());
        mediaPlayer = new MediaPlayer(media);
        mediaPlayer.setVolume(volumeSlider.getValue() / 100.0);

        mediaPlayer.setOnEndOfMedia(this::playNext);
        mediaPlayer.play();
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
        currentTrackLabel.setText("Нажмите ЗАПУСТИТЬ");
        playButton.setText("ЗАПУСТИТЬ");
        playButton.setOnAction(e -> playRandomTrack());
    }

    @FXML
    private void playPrevious() {
        if (trackFiles.isEmpty() || currentTrackIndex == -1) return;
        stopCurrentTrack();
        currentTrackIndex = (currentTrackIndex - 1 + trackFiles.size()) % trackFiles.size();
        playTrack(trackFiles.get(currentTrackIndex));
    }

    @FXML
    private void playNext() {
        if (trackFiles.isEmpty() || currentTrackIndex == -1) return;
        stopCurrentTrack();
        currentTrackIndex = (currentTrackIndex + 1) % trackFiles.size();
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
        Label placeholder = new Label(text);
        placeholder.setStyle("-fx-font-size: 18; -fx-text-fill: #95a5a6; -fx-padding: 40 0 0 0;");
        placeholder.setWrapText(true);
        container.getChildren().setAll(placeholder);
    }

    private void loadAfisha() {
        if (currentUser == null) return;
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
        card.getChildren().addAll(title, date, loc);
        return card;
    }


    private void loadFavoriteTracksSection() {
        if (currentUser == null || favoriteContentContainer == null) return;

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
                        rs.getString("ArtistName")
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

    private void loadFavoriteTracks() {
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
                        rs.getString("ArtistName")
                );
                favoriteContentContainer.getChildren().add(card);
            }
            if (!has) showPlaceholder(favoriteContentContainer, "Нет любимых треков");
        } catch (SQLException e) {
            e.printStackTrace();
            showPlaceholder(favoriteContentContainer, "Ошибка загрузки треков");
        }
    }

    private void loadFavoritePlaylistsSection() {
        if (currentUser == null || favoritePlaylistsContainer == null) return;

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

        Label d = new Label(date != null ? date.substring(0, 10) : "");
        d.setStyle("-fx-text-fill: #fadbd8; -fx-font-size: 11;");

        card.getChildren().addAll(t, o, d);
        card.setOnMouseClicked(e -> System.out.println("Открыть плейлист: " + title));
        return card;
    }

    private VBox createTrackCard(int trackId, String title, String artist) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 12; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 3); -fx-cursor: hand;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 15; -fx-text-fill: #2c3e50;");

        Label artistLabel = new Label(artist != null ? artist : "Неизвестный исполнитель");
        artistLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 13;");

        card.getChildren().addAll(titleLabel, artistLabel);
        card.setOnMouseClicked(e -> playTrack(trackId, title, artist));
        return card;
    }

    private void playTrack(int trackId, String title, String artist) {
        System.out.println("Плеер: сейчас играет → " + title + " (" + artist + ")");
    }

    private void loadUserPlaylists() {
        if (currentUser == null || userPlaylistsContainer == null) return;
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
        }
    }

    private VBox createPlaylistCard(String title, String date) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 14; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 8, 0, 0, 2); -fx-cursor: hand;");

        Label t = new Label(title);
        t.setStyle("-fx-font-weight: bold; -fx-font-size: 15; -fx-text-fill: #2c3e50;");

        Label d = new Label("Создан: " + (date != null ? date.substring(0, 10) : "Недавно"));
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
}