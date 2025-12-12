package org.example.vp_final;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.*;
import java.util.Base64;

public class DatabaseHelper {

    private static final String DB_URL = "jdbc:sqlite:music_app.db";

    public static void initDatabase() {
        String[] sqlTables = {
                // Пользователи
                """
                CREATE TABLE IF NOT EXISTS User (
                    UserID INTEGER PRIMARY KEY AUTOINCREMENT,
                    Username NVARCHAR(100) NOT NULL UNIQUE,
                    Email NVARCHAR(300) NOT NULL UNIQUE,
                    PasswordHash NVARCHAR(500) NOT NULL
                );
                """,

                // Жанры
                """
                CREATE TABLE IF NOT EXISTS Genre (
                    GenreID INTEGER PRIMARY KEY AUTOINCREMENT,
                    Name NVARCHAR(500) NOT NULL UNIQUE,
                    Description NVARCHAR(500)
                );
                """,

                // Исполнители
                """
                CREATE TABLE IF NOT EXISTS Artist (
                    ArtistID INTEGER PRIMARY KEY AUTOINCREMENT,
                    Name NVARCHAR(300) NOT NULL UNIQUE,
                    GenreID INTEGER,
                    FOREIGN KEY(GenreID) REFERENCES Genre(GenreID)
                );
                """,

                // Альбомы
                """
                CREATE TABLE IF NOT EXISTS Album (
                    AlbumID INTEGER PRIMARY KEY AUTOINCREMENT,
                    Title NVARCHAR(500) NOT NULL,
                    ArtistID INTEGER,
                    ReleaseDate NVARCHAR(500),
                    FOREIGN KEY (ArtistID) REFERENCES Artist(ArtistID) ON DELETE SET NULL
                );
                """,

                // Треки
                """
                CREATE TABLE IF NOT EXISTS Track (
                    TrackID INTEGER PRIMARY KEY AUTOINCREMENT,
                    Title NVARCHAR(500) NOT NULL,
                    ArtistID INTEGER,
                    AlbumID INTEGER,
                    Duration INTEGER,
                    TrackURL NVARCHAR(1000),
                    GenreID INTEGER,
                    FOREIGN KEY (ArtistID) REFERENCES Artist(ArtistID) ON DELETE SET NULL,
                    FOREIGN KEY (AlbumID) REFERENCES Album(AlbumID) ON DELETE SET NULL
                );
                """,

                // Плейлисты
                """
                CREATE TABLE IF NOT EXISTS Playlist (
                    PlaylistID INTEGER PRIMARY KEY AUTOINCREMENT,
                    UserID INTEGER,
                    Title NVARCHAR(300) NOT NULL,
                    CreationDate NVARCHAR(500) DEFAULT (datetime('now')),
                    FOREIGN KEY (UserID) REFERENCES User(UserID) ON DELETE CASCADE
                );
                """,

                // Афиша
                """
                CREATE TABLE IF NOT EXISTS Afisha (
                    AfishaID INTEGER PRIMARY KEY AUTOINCREMENT,
                    Title NVARCHAR(500) NOT NULL,
                    Date NVARCHAR(500) NOT NULL,
                    Location NVARCHAR(300),
                    Description NVARCHAR(400)
                );
                """,

                // Треки в плейлистах
                """
                CREATE TABLE IF NOT EXISTS PlaylistTrack (
                    PlaylistID INTEGER,
                    TrackID INTEGER,
                    PRIMARY KEY (PlaylistID, TrackID),
                    FOREIGN KEY (PlaylistID) REFERENCES Playlist(PlaylistID) ON DELETE CASCADE,
                    FOREIGN KEY (TrackID) REFERENCES Track(TrackID) ON DELETE CASCADE
                );
                """,

                // Лайки треков
                """
                CREATE TABLE IF NOT EXISTS UserLike (
                    UserID INTEGER,
                    TrackID INTEGER,
                    PRIMARY KEY (UserID, TrackID),
                    FOREIGN KEY (UserID) REFERENCES User(UserID) ON DELETE CASCADE,
                    FOREIGN KEY (TrackID) REFERENCES Track(TrackID) ON DELETE CASCADE
                );
                """,

                // Любимые авторы
                """
                CREATE TABLE IF NOT EXISTS UserLikeAuthor (
                    UserID INTEGER,
                    ArtistID INTEGER,
                    PRIMARY KEY (UserID, ArtistID),
                    FOREIGN KEY (UserID) REFERENCES User(UserID) ON DELETE CASCADE,
                    FOREIGN KEY (ArtistID) REFERENCES Artist(ArtistID) ON DELETE CASCADE
                );
                """,

                // Любимые плейлисты
                """
                CREATE TABLE IF NOT EXISTS UserLikePlaylist (
                    UserLikePlaylistID INTEGER PRIMARY KEY AUTOINCREMENT,
                    PlaylistID INTEGER NOT NULL,
                    UserID INTEGER NOT NULL,
                    FOREIGN KEY (PlaylistID) REFERENCES Playlist(PlaylistID) ON DELETE CASCADE,
                    FOREIGN KEY (UserID) REFERENCES User(UserID) ON DELETE CASCADE,
                    UNIQUE(PlaylistID, UserID)
                );
                """,

                // Настройки приложения
                """
                CREATE TABLE IF NOT EXISTS Settings (
                    Key NVARCHAR(200) PRIMARY KEY,
                    Value NVARCHAR(500)
                );
                """
        };

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");

            for (String sql : sqlTables) {
                stmt.execute(sql);
            }

            System.out.println("База данных инициализирована успешно.");
        } catch (SQLException e) {
            System.err.println("Ошибка при инициализации БД: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    private static String hashPassword(String password, String saltBase64) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(Base64.getDecoder().decode(saltBase64));
            byte[] hashed = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hashed);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка хеширования пароля", e);
        }
    }

    public static boolean registerUser(String username, String email, String password) {
        String salt = generateSalt();
        String hash = hashPassword(password, salt);

        String sql = "INSERT INTO User (Username, Email, PasswordHash) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, email);
            pstmt.setString(3, hash + ":" + salt);

            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Регистрация не удалась: " + e.getMessage());
            return false;
        }
    }

    public static User loginUser(String usernameOrEmail, String password) {
        String sql = "SELECT UserID, Username, Email, PasswordHash FROM User WHERE Username = ? OR Email = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, usernameOrEmail);
            pstmt.setString(2, usernameOrEmail);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String stored = rs.getString("PasswordHash");
                    String[] parts = stored.split(":");
                    if (parts.length != 2) return null;

                    String storedHash = parts[0];
                    String salt = parts[1];

                    if (storedHash.equals(hashPassword(password, salt))) {
                        return new User(
                                rs.getInt("UserID"),
                                rs.getString("Username"),
                                rs.getString("Email")
                        );
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Ошибка входа: " + e.getMessage());
        }
        return null;
    }

    public static User getUserById(int userId) {
        String sql = "SELECT UserID, Username, Email FROM User WHERE UserID = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getInt("UserID"),
                            rs.getString("Username"),
                            rs.getString("Email")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // === Работа с лайками треков ===

    public static boolean isTrackLiked(int userId, int trackId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM UserLike WHERE UserID = ? AND TrackID = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, trackId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    public static void addToFavorites(int userId, int trackId) throws SQLException {
        String sql = "INSERT OR IGNORE INTO UserLike (UserID, TrackID) VALUES (?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, trackId);
            pstmt.executeUpdate();
        }
    }

    public static void removeFromFavorites(int userId, int trackId) throws SQLException {
        String sql = "DELETE FROM UserLike WHERE UserID = ? AND TrackID = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, trackId);
            pstmt.executeUpdate();
        }
    }

    // === Работа с настройками окна ===

    private static void saveSetting(String key, String value) {
        String sql = "INSERT OR REPLACE INTO Settings (Key, Value) VALUES (?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, key);
            pstmt.setString(2, value);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Ошибка сохранения настройки: " + key);
        }
    }

    private static String loadSetting(String key, String defaultValue) {
        String sql = "SELECT Value FROM Settings WHERE Key = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, key);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("Value");
                }
            }
        } catch (SQLException e) {
            System.err.println("Ошибка загрузки настройки: " + key);
        }
        return defaultValue;
    }

    public static void saveWindowState(double x, double y, double width, double height) {
        saveSetting("window_x", String.valueOf(x));
        saveSetting("window_y", String.valueOf(y));
        saveSetting("window_width", String.valueOf(width));
        saveSetting("window_height", String.valueOf(height));
    }

    public static double[] loadWindowState() {
        double x = parseDoubleSafe(loadSetting("window_x", "100.0"), 100.0);
        double y = parseDoubleSafe(loadSetting("window_y", "100.0"), 100.0);
        double width = parseDoubleSafe(loadSetting("window_width", "800.0"), 800.0);
        double height = parseDoubleSafe(loadSetting("window_height", "700.0"), 700.0);

        if (width < 300) width = 300;
        if (height < 300) height = 300;

        return new double[]{x, y, width, height};
    }

    private static double parseDoubleSafe(String value, double defaultValue) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            System.err.println("Ошибка парсинга double: " + value);
            return defaultValue;
        }
    }

    public static void saveLastLoggedInUser(int userId) {
        saveSetting("last_user_id", String.valueOf(userId));
    }

    public static int loadLastLoggedInUserId() {
        String value = loadSetting("last_user_id", "-1");
        try {
            // Если в БД хранится дробное число (например "1.0"), сначала парсим как double
            if (value.contains(".")) {
                return (int) Double.parseDouble(value);
            }
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            System.err.println("Ошибка парсинга last_user_id: " + value);
            return -1;
        }
    }

    // === Работа с плейлистами ===

    public static boolean createPlaylist(int userId, String title) {
        String sql = "INSERT INTO Playlist (UserID, Title) VALUES (?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, title);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Ошибка создания плейлиста: " + title + " для UserID=" + userId);
            e.printStackTrace();
            return false;
        }
    }
}