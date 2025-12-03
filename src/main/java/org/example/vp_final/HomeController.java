//package org.example.vp_final;
//
//import javafx.fxml.FXML;
//import javafx.fxml.Initializable;
//import javafx.geometry.Insets;
//import javafx.scene.control.Label;
//import javafx.scene.control.ChoiceBox; // <-- ДОБАВЛЕНО
//import javafx.scene.layout.FlowPane;
//import javafx.scene.layout.VBox;
//import java.net.URL;
//import java.sql.SQLException;
//import java.sql.Connection;
//import java.sql.DriverManager;
//import java.sql.PreparedStatement;
//import java.sql.ResultSet;
//import java.util.ResourceBundle;
//import java.util.Comparator;
//import java.util.List;
//import java.util.ArrayList;
//
//public class HomeController implements Initializable {
//
//    @FXML private FlowPane newTracksContainer;

//    @FXML private FlowPane userPlaylistsContainer;
//    @FXML private FlowPane afishaContainer;
//    @FXML private ChoiceBox<String> afishaSortColumn;
//    @FXML private ChoiceBox<String> afishaSortDirection;
//    @FXML private ToggleButton toggleTracks;
//    @FXML private ToggleButton togglePlaylists;
//    @FXML private ToggleGroup favoriteToggleGroup;
//    @FXML private FlowPane favoriteContentContainer;
//
//    // Хранилище данных
//    private List<AfishaEvent> afishaEvents = new ArrayList<>();
//
//    private User currentUser;
//    private MainController mainController;
//
//    public void setMainController(MainController mainController) {
//        this.mainController = mainController;
//    }
//    public void setUser(User user) {
//        this.currentUser = user;
//        loadUserPlaylists();
//        loadAfisha();
//        loadFavoriteSection();           // <-- заменяет старый loadFavoriteTracks()
//    }
//
//    @Override
//    public void initialize(URL location, ResourceBundle resources) {
//        loadLatestTracks();
//        setupAfishaSorting();
//
//        if (toggleTracks != null) toggleTracks.setSelected(true);
//        favoriteToggleGroup.selectedToggleProperty().addListener((obs, old, newToggle) -> {
//            if (newToggle == null) old.setSelected(true);
//            else loadFavoriteSection();
//        });
//    }
//    }
//
//    private void loadLatestTracks() {
//        newTracksContainer.getChildren().clear();
//
//        String sql = """
//    SELECT t.TrackID, t.Title, a.Name AS ArtistName, t.Duration
//    FROM Track t
//    JOIN Artist a ON t.ArtistID = a.ArtistID
//    ORDER BY t.TrackID DESC  -- Сортировка по ID трека, чтобы получить "новые"
//    LIMIT 6
//    """;
//
//        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:music_app.db");
//             PreparedStatement pstmt = conn.prepareStatement(sql);
//             ResultSet rs = pstmt.executeQuery()) {
//
//            boolean hasTracks = false;
//
//            while (rs.next()) {
//                hasTracks = true;
//
//                int trackId = rs.getInt("TrackID");
//                String title = rs.getString("Title");
//                String artist = rs.getString("ArtistName");
//                if (artist == null) artist = "Неизвестный исполнитель";
//
//                // Используем уже существующий метод для создания карточки
//                VBox card = createTrackCard(trackId, title, artist);
//                newTracksContainer.getChildren().add(card);
//            }
//
//            if (!hasTracks) {
//                showPlaceholder(newTracksContainer, "В разделе 'Новое' пока нет треков.");
//            }
//
//        } catch (SQLException e) {
//            e.printStackTrace();
//            showPlaceholder(newTracksContainer, "Ошибка загрузки новых треков.");
//        }
//    }
//
//
//
//    private void setupAfishaSorting() {
//        // Заполнение ChoiceBox для столбца
//        afishaSortColumn.getItems().addAll("Название", "Дата", "Место");
//        afishaSortColumn.setValue("Дата");
//
//        // Заполнение ChoiceBox для направления
//        afishaSortDirection.getItems().addAll("↑ Возрастание", "↓ Убывание");
//        afishaSortDirection.setValue("↑ Возрастание");
//
//        // Добавление слушателей для автоматической сортировки при изменении выбора
//        afishaSortColumn.valueProperty().addListener((obs, oldVal, newVal) -> sortAndDisplayAfisha());
//        afishaSortDirection.valueProperty().addListener((obs, oldVal, newVal) -> sortAndDisplayAfisha());
//    }
//
//    // ИСПРАВЛЕННЫЙ метод showPlaceholder
//    private void showPlaceholder(FlowPane container, String text) {
//        Label placeholder = new Label(text);
//        placeholder.setStyle("-fx-font-size: 18; -fx-text-fill: #95a5a6; -fx-padding: 40 0 0 0;");
//        placeholder.setWrapText(true);
//        container.getChildren().clear(); // Очищаем старое содержимое
//        container.getChildren().add(placeholder);
//    }
//
//    // ГЛАВНЫЙ метод loadAfisha (единственный, который остался)
//    private void loadAfisha() {
//        if (currentUser == null) return;
//        afishaEvents.clear();
//
//        String sql = """
//            SELECT AfishaID, Title, Date, Location
//            FROM Afisha
//            ORDER BY Date DESC
//            LIMIT 10
//            """;
//
//        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:music_app.db");
//             PreparedStatement pstmt = conn.prepareStatement(sql);
//             ResultSet rs = pstmt.executeQuery()) {
//
//            while (rs.next()) {
//                int id = rs.getInt("AfishaID");
//                String title = rs.getString("Title");
//                String date = rs.getString("Date");
//                String location = rs.getString("Location");
//
//                afishaEvents.add(new AfishaEvent(id, title, date, location));
//            }
//
//            if (afishaEvents.isEmpty()) {
//                showPlaceholder(afishaContainer, "Предстоящих событий нет."); // ИСПРАВЛЕННЫЙ ВЫЗОВ
//            } else {
//                sortAndDisplayAfisha();
//            }
//
//        } catch (SQLException e) {
//            e.printStackTrace();
//            showPlaceholder(afishaContainer, "Ошибка загрузки афиши."); // ИСПРАВЛЕННЫЙ ВЫЗОВ
//        }
//    }
//
//    // Метод для подготовки сортировки и вызова отображения
//    private void sortAndDisplayAfisha() {
//        if (afishaEvents.isEmpty()) return;
//
//        String column = afishaSortColumn.getValue();
//        String direction = afishaSortDirection.getValue();
//        boolean ascending = "↑ Возрастание".equals(direction);
//
//        // Создаем Comparator на основе выбранного столбца
//        Comparator<AfishaEvent> comparator = switch (column) {
//            case "Название" -> Comparator.comparing(AfishaEvent::title);
//            case "Место" -> Comparator.comparing(AfishaEvent::location);
//            case "Дата" -> Comparator.comparing(AfishaEvent::date);
//            default -> Comparator.comparing(AfishaEvent::afishaId); // Запасной вариант
//        };
//
//        if (!ascending) {
//            comparator = comparator.reversed();
//        }
//
//        // Вызываем Quicksort
//        quicksort(afishaEvents, comparator);
//
//        displayAfisha();
//    }
//
//    // Метод для отображения отсортированных данных
//    private void displayAfisha() {
//        afishaContainer.getChildren().clear();
//        for (AfishaEvent event : afishaEvents) {
//            VBox card = createAfishaCard(event);
//            afishaContainer.getChildren().add(card);
//        }
//    }
//
//    // Метод создания карточки события
//    private VBox createAfishaCard(AfishaEvent event) {
//        VBox card = new VBox(8);
//        card.setPrefSize(180, 180);
//        card.setStyle("-fx-background-color: #ecf0f1; -fx-padding: 15; -fx-background-radius: 12; -fx-cursor: hand;");
//
//        Label titleLabel = new Label(event.title());
//        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 15; -fx-text-fill: #2c3e50;");
//
//        Label dateLabel = new Label("Когда: " + event.date());
//        dateLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 13;");
//
//        Label locationLabel = new Label("Где: " + event.location());
//        locationLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 13;");
//
//        card.getChildren().addAll(titleLabel, dateLabel, locationLabel);
//        card.setOnMouseClicked(e -> System.out.println("Открыть событие AfishaID=" + event.afishaId()));
//
//        return card;
//    }
//
//    private void loadUserPlaylists() {
//        if (currentUser == null || userPlaylistsContainer == null) return;
//
//        userPlaylistsContainer.getChildren().clear();
//
//        String sql = """
//            SELECT PlaylistID, Title, CreationDate
//            FROM Playlist
//            WHERE UserID = ?
//            ORDER BY CreationDate DESC
//            """;
//
//        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:music_app.db");
//             PreparedStatement pstmt = conn.prepareStatement(sql)) {
//
//            pstmt.setInt(1, currentUser.userId());
//            ResultSet rs = pstmt.executeQuery();
//
//            boolean hasPlaylists = false;
//
//            while (rs.next()) {
//                hasPlaylists = true;
//                int id = rs.getInt("PlaylistID");
//                String title = rs.getString("Title");
//                String date = rs.getString("CreationDate");
//
//                VBox card = createPlaylistCard(id, title, date);
//                userPlaylistsContainer.getChildren().add(card);
//            }
//
//            if (!hasPlaylists) {
//                Label empty = new Label("У вас пока нет плейлистов");
//                empty.setStyle("-fx-font-size: 16; -fx-text-fill: #95a5a6; -fx-padding: 30;");
//                userPlaylistsContainer.getChildren().add(empty);
//            }
//
//        } catch (SQLException e) {
//            e.printStackTrace();
//            Label error = new Label("Ошибка загрузки плейлистов");
//            userPlaylistsContainer.getChildren().add(error);
//        }
//    }
//
//    private VBox createPlaylistCard(int id, String title, String date) {
//        VBox card = new VBox(10);
//        card.setPadding(new Insets(16));
//        card.setStyle("""
//            -fx-background-color: #ffffff;
//            -fx-background-radius: 14;
//            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 10, 0.2, 0, 3);
//            -fx-pref-width: 220;
//            -fx-cursor: hand;
//            """);
//
//        Label titleLabel = new Label(title);
//        titleLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
//        titleLabel.setWrapText(true);
//
//        Label dateLabel = new Label("Создан: " + formatDate(date));
//        dateLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #7f8c8d;");
//
//        card.getChildren().addAll(titleLabel, dateLabel);
//
//        // При клике — можно открыть плейлист (в будущем)
//        card.setOnMouseClicked(e -> {
//            System.out.println("Открыт плейлист: " + title + " (ID: " + id + ")");
//            // Здесь потом можно открыть экран с содержимым плейлиста
//        });
//
//        return card;
//    }
//
//    private String formatDate(String sqliteDate) {
//        if (sqliteDate == null) return "Неизвестно";
//        try {
//            return sqliteDate.substring(0, 10); // "2025-12-25 10:30:00" → "2025-12-25"
//        } catch (Exception e) {
//            return sqliteDate;
//        }
//    }
//
//    public void refreshPlaylists() {
//        loadUserPlaylists();
//    }
//
//    private void loadFavoriteSection() {
//        if (currentUser == null || favoriteContentContainer == null) return;
//
//        favoriteContentContainer.getChildren().clear();
//
//        if (toggleTracks != null && toggleTracks.isSelected()) {
//            loadFavoriteTracks();        // старый метод (треки)
//        } else {
//            loadFavoritePlaylists();     // новый метод (плейлисты)
//        }
//    }
//
//private void loadFavoritePlaylists() {
//    String sql = """
//        SELECT p.PlaylistID, p.Title, p.CreationDate, u.Username AS OwnerName
//        FROM Playlist p
//        JOIN UserLikePlaylist ulp ON p.PlaylistID = ulp.PlaylistID
//        JOIN User u ON p.UserID = u.UserID
//        WHERE ulp.UserID = ?
//        ORDER BY p.CreationDate DESC
//        """;
//
//    try (Connection conn = DriverManager.getConnection("jdbc:sqlite:music_app.db");
//         PreparedStatement pstmt = conn.prepareStatement(sql)) {
//
//        pstmt.setInt(1, currentUser.userId());
//        ResultSet rs = pstmt.executeQuery();
//
//        boolean has = false;
//        while (rs.next()) {
//            has = true;
//            String title = rs.getString("Title");
//            String owner = rs.getString("OwnerName");
//            String date  = rs.getString("CreatedAt");
//
//            VBox card = createFavoritePlaylistCard(title, owner, date);
//            favoriteContentContainer.getChildren().add(card);
//        }
//
//        if (!has) {
//            showPlaceholder(favoriteContentContainer, "Пока нет понравившихся плейлистов");
//        }
//
//    } catch (SQLException e) {
//        e.printStackTrace();
//        showPlaceholder(favoriteContentContainer, "Ошибка загрузки плейлистов");
//    }
//}
//
//    private void loadFavoriteTracks() {
//        favoriteTracksContainer.getChildren().clear();
//
//        if (currentUser == null) {
//            showPlaceholder(favoriteTracksContainer, "Войдите в аккаунт"); // ИСПРАВЛЕННЫЙ ВЫЗОВ
//            return;
//        }
//
//        String sql = """
//        SELECT t.TrackID, t.Title, a.Name AS ArtistName, t.Duration
//        FROM Track t
//        JOIN Artist a ON t.ArtistID = a.ArtistID
//        JOIN UserLike ul ON t.TrackID = ul.TrackID
//        WHERE ul.UserID = ?
//        ORDER BY t.Title
//        """;
//
//        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:music_app.db");
//             PreparedStatement pstmt = conn.prepareStatement(sql)) {
//
//            pstmt.setInt(1, currentUser.userId());
//            ResultSet rs = pstmt.executeQuery();
//
//            boolean hasTracks = false;
//
//            while (rs.next()) {
//                hasTracks = true;
//
//                int trackId = rs.getInt("TrackID");
//                String title = rs.getString("Title");
//                String artist = rs.getString("ArtistName");
//                if (artist == null) artist = "Неизвестный исполнитель";
//
//                VBox card = createTrackCard(trackId, title, artist);
//                favoriteTracksContainer.getChildren().add(card);
//            }
//
//            if (!hasTracks) {
//                showPlaceholder(favoriteTracksContainer, "Тут будут твои любимые треки, " + currentUser.username()); // ИСПРАВЛЕННЫЙ ВЫЗОВ
//            }
//
//        } catch (SQLException e) {
//            e.printStackTrace();
//            showPlaceholder(favoriteTracksContainer, "Ошибка загрузки любимых треков"); // ИСПРАВЛЕННЫЙ ВЫЗОВ
//        }
//    }
//
//    private VBox createTrackCard(int trackId, String title, String artist) {
//        VBox card = new VBox(8);
//        card.setPadding(new Insets(16));
//        card.setStyle("""
//        -fx-background-color: #ffffff;
//        -fx-background-radius: 12;
//        -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 3);
//        -fx-cursor: hand;
//        """);
//
//        Label titleLabel = new Label(title);
//        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 15; -fx-text-fill: #2c3e50;");
//
//        Label artistLabel = new Label(artist);
//        artistLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 13;");
//
//        card.getChildren().addAll(titleLabel, artistLabel);
//
//        card.setOnMouseClicked(e -> {
//            System.out.println("Воспроизвести трек ID=" + trackId + ": " + title + " — " + artist);
//            playTrack(trackId, title, artist);
//        });
//
//        return card;
//    }
//
//    // 6. Карточка любимого плейлиста
//    private VBox createFavoritePlaylistCard(String title, String owner, String date) {
//        VBox card = new VBox(10);
//        card.setPadding(new Insets(18));
//        card.setMinWidth(220);
//        card.setStyle("-fx-background-color: #e74c3c; -fx-background-radius: 18; " +
//                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 12, 0.3, 0, 4); -fx-cursor: hand;");
//
//        Label t = new Label(title);
//        t.setStyle("-fx-font-weight: bold; -fx-font-size: 17; -fx-text-fill: white;");
//        t.setWrapText(true);
//
//        Label o = new Label("от " + (owner != null ? owner : "Аноним"));
//        o.setStyle("-fx-text-fill: #fadbd8; -fx-font-size: 13;");
//
//        Label d = new Label(date != null ? date.substring(0, 10) : "");
//        d.setStyle("-fx-text-fill: #fadbd8; -fx-font-size: 11;");
//
//        card.getChildren().addAll(t, o, d);
//        card.setOnMouseClicked(e -> System.out.println("Открыть плейлист: " + title));
//        return card;
//    }
//
//    // ====================================================================
//    // АЛГОРИТМ СОРТИРОВКИ QUICKSORT (с улучшениями)
//    // ====================================================================
//
//    private void quicksort(List<AfishaEvent> list, Comparator<AfishaEvent> comparator) {
//        quicksort(list, 0, list.size() - 1, comparator);
//    }
//
//    private void quicksort(List<AfishaEvent> list, int low, int high, Comparator<AfishaEvent> comparator) {
//        if (low < high) {
//            // Оптимизация 1: Сортировка вставками для малых подмассивов (порог 10)
//            if (high - low < 10) {
//                insertionSort(list, low, high, comparator);
//                return;
//            }
//
//            // Разделение массива и получение индекса опорного элемента
//            int pivotIndex = partition(list, low, high, comparator);
//
//            // Рекурсивная сортировка подмассивов
//            quicksort(list, low, pivotIndex - 1, comparator);
//            quicksort(list, pivotIndex + 1, high, comparator);
//        }
//    }
//
//    // Разделение (Partition)
//    private int partition(List<AfishaEvent> list, int low, int high, Comparator<AfishaEvent> comparator) {
//        // Оптимизация 2: Медиана из трех для выбора опорного элемента
//        int medianIndex = medianOfThree(list, low, high, comparator);
//        swap(list, medianIndex, high); // Перемещаем медиану в конец (на место опорного элемента)
//
//        AfishaEvent pivot = list.get(high);
//        int i = (low - 1); // Индекс меньшего элемента
//
//        for (int j = low; j < high; j++) {
//            // Если текущий элемент меньше или равен опорному
//            if (comparator.compare(list.get(j), pivot) <= 0) {
//                i++;
//                swap(list, i, j);
//            }
//        }
//
//        // Помещаем опорный элемент на правильное место
//        swap(list, i + 1, high);
//
//        return i + 1;
//    }
//
//    // Вспомогательный метод: Выбор медианы из трех (улучшение Quicksort)
//    private int medianOfThree(List<AfishaEvent> list, int low, int high, Comparator<AfishaEvent> comparator) {
//        int center = low + (high - low) / 2;
//
//        // Сортируем low, center и high, чтобы center стал медианой
//        if (comparator.compare(list.get(low), list.get(center)) > 0)
//            swap(list, low, center);
//
//        if (comparator.compare(list.get(low), list.get(high)) > 0)
//            swap(list, low, high);
//
//        if (comparator.compare(list.get(center), list.get(high)) > 0)
//            swap(list, center, high);
//
//        return center;
//    }
//
//    // Вспомогательный метод: Обмен элементов
//    private void swap(List<AfishaEvent> list, int i, int j) {
//        AfishaEvent temp = list.get(i);
//        list.set(i, list.get(j));
//        list.set(j, temp);
//    }
//
//    // Вспомогательный метод: Сортировка вставками (для малых подмассивов)
//    private void insertionSort(List<AfishaEvent> list, int low, int high, Comparator<AfishaEvent> comparator) {
//        for (int i = low + 1; i <= high; i++) {
//            AfishaEvent key = list.get(i);
//            int j = i - 1;
//
//            while (j >= low && comparator.compare(list.get(j), key) > 0) {
//                list.set(j + 1, list.get(j));
//                j = j - 1;
//            }
//            list.set(j + 1, key);
//        }
//    }
//
//    // Заглушка для будущего плеера
//    private void playTrack(int trackId, String title, String artist) {
//        System.out.println("Плеер: сейчас играет → " + title + " (" + artist + ")");
//    }
//
//    @FXML
//    private void onMainAction() {
//        System.out.println("Запуск основного действия для пользователя: " +
//                (currentUser != null ? currentUser.username() : "гость"));
//    }
//
//    @FXML
//    private void openProfile() {
//        if (mainController != null) {
//            mainController.showProfile();
//        }
//    }
//}
package org.example.vp_final;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.sql.*;
import java.util.*;
import java.util.ResourceBundle;

public class HomeController implements Initializable {

    // === Контейнеры ===
    @FXML private FlowPane newTracksContainer;
    @FXML private FlowPane userPlaylistsContainer;
    @FXML private FlowPane afishaContainer;
    @FXML private FlowPane favoriteContentContainer;  // ЕДИНЫЙ контейнер для "Любимое"

    // === Афиша ===
    @FXML private ChoiceBox<String> afishaSortColumn;
    @FXML private ChoiceBox<String> afishaSortDirection;

    // === Переключатель "Любимое" ===
    @FXML private ToggleButton toggleTracks;
    @FXML private ToggleButton togglePlaylists;
    @FXML private ToggleGroup favoriteToggleGroup;

    // Хранилище данных
    private List<AfishaEvent> afishaEvents = new ArrayList<>();
    private User currentUser;
    private MainController mainController;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    public void setUser(User user) {
        this.currentUser = user;
        loadUserPlaylists();
        loadAfisha();
        loadFavoriteSection();  // Универсальная загрузка "Любимое"
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadLatestTracks();
        setupAfishaSorting();

        // По умолчанию — показываем треки
        if (toggleTracks != null) {
            toggleTracks.setSelected(true);
        }

        // Самое главное — правильный слушатель!
        favoriteToggleGroup.selectedToggleProperty().addListener((obs, old, selectedToggle) -> {
            loadFavoriteSection(); // просто вызываем один метод — он сам разберётся
        });

        // Загружаем контент при первом открытии экрана
        loadFavoriteSection();
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

    // === Любимое: универсальная загрузка ===
    private void loadFavoriteSection() {
        if (currentUser == null || favoriteContentContainer == null) return;

        favoriteContentContainer.getChildren().clear(); // очищаем старое

        if (toggleTracks.isSelected()) {
            loadFavoriteTracks();
        } else {
            loadFavoritePlaylists(); // сюда попадём, когда выбран "Плейлисты"
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

    private void loadFavoritePlaylists() {
        String sql = """
        SELECT p.PlaylistID, p.Title, p.CreatedAt, u.Username AS OwnerName
        FROM Playlist p
        JOIN UserLikePlaylist ulp ON p.PlaylistID = ulp.PlaylistID
        JOIN User u ON p.UserID = u.UserID
        WHERE ulp.UserID = ?
        ORDER BY p.CreatedAt DESC
        """;

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:music_app.db");
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, currentUser.userId());
            ResultSet rs = pstmt.executeQuery();

            boolean has = false;
            while (rs.next()) {
                has = true;
                VBox card = createFavoritePlaylistCard(
                        rs.getString("Title"),
                        rs.getString("OwnerName"),
                        rs.getString("CreatedAt")
                );
                favoriteContentContainer.getChildren().add(card);
            }

            if (!has) {
                showPlaceholder(favoriteContentContainer, "Нет понравившихся плейлистов");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showPlaceholder(favoriteContentContainer, "Ошибка загрузки");
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

    @FXML private void openProfile() {
        if (mainController != null) mainController.showProfile();
    }
}