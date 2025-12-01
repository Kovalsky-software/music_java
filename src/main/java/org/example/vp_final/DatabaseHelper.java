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
                    Username TEXT NOT NULL UNIQUE,
                    Email TEXT NOT NULL UNIQUE,
                    PasswordHash TEXT NOT NULL
                );
                """,

                // Исполнители
                """
                CREATE TABLE IF NOT EXISTS Artist (
                    ArtistID INTEGER PRIMARY KEY AUTOINCREMENT,
                    Name TEXT NOT NULL
                );
                """,

                // Альбомы
                """
                CREATE TABLE IF NOT EXISTS Album (
                    AlbumID INTEGER PRIMARY KEY AUTOINCREMENT,
                    Title TEXT NOT NULL,
                    ArtistID INTEGER,
                    ReleaseDate TEXT,
                    FOREIGN KEY (ArtistID) REFERENCES Artist(ArtistID) ON DELETE SET NULL
                );
                """,

                // добавление жанров в поиске
                """
                CREATE TABLE IF NOT EXISTS Genre (
                    GenreID INTEGER PRIMARY KEY AUTOINCREMENT,
                    Name TEXT NOT NULL UNIQUE,
                    Description TEXT
                );
                """,

                // Треки
                """
                CREATE TABLE IF NOT EXISTS Track (
                    TrackID INTEGER PRIMARY KEY AUTOINCREMENT,
                    Title TEXT NOT NULL,
                    ArtistID INTEGER,
                    AlbumID INTEGER,
                    Duration INTEGER,
                    GenreID INTEGER,
                    TrackURL TEXT,
                    FOREIGN KEY (ArtistID) REFERENCES Artist(ArtistID) ON DELETE SET NULL,
                    FOREIGN KEY (AlbumID) REFERENCES Album(AlbumID) ON DELETE SET NULL
                );
                """,

                // Плейлисты
                """
                CREATE TABLE IF NOT EXISTS Playlist (
                    PlaylistID INTEGER PRIMARY KEY AUTOINCREMENT,
                    UserID INTEGER,
                    Title TEXT NOT NULL,
                    CreationDate TEXT DEFAULT (datetime('now')),
                    FOREIGN KEY (UserID) REFERENCES User(UserID) ON DELETE CASCADE
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

                // Лайки
                """
                CREATE TABLE IF NOT EXISTS UserLike (
                    UserID INTEGER,
                    TrackID INTEGER,
                    PRIMARY KEY (UserID, TrackID),
                    FOREIGN KEY (UserID) REFERENCES User(UserID) ON DELETE CASCADE,
                    FOREIGN KEY (TrackID) REFERENCES Track(TrackID) ON DELETE CASCADE
                );
                """,

                // Планы подписки
                """
                CREATE TABLE IF NOT EXISTS SubscriptionPlan (
                    PlanID INTEGER PRIMARY KEY AUTOINCREMENT,
                    Name TEXT NOT NULL UNIQUE,
                    Price REAL NOT NULL,
                    Description TEXT
                );
                """,

                // Платежи
                """
                CREATE TABLE IF NOT EXISTS Payment (
                    PaymentID INTEGER PRIMARY KEY AUTOINCREMENT,
                    UserID INTEGER NOT NULL,
                    PlanID INTEGER NOT NULL,
                    Amount REAL NOT NULL,
                    PaymentDate TEXT DEFAULT (datetime('now')),
                    Method TEXT,
                    FOREIGN KEY (UserID) REFERENCES User(UserID) ON DELETE CASCADE,
                    FOREIGN KEY (PlanID) REFERENCES SubscriptionPlan(PlanID)
                );
                """,

                // Активные подписки
                """
                CREATE TABLE IF NOT EXISTS UserSubscription (
                    UserID INTEGER,
                    PlanID INTEGER,
                    StartDate TEXT NOT NULL,
                    EndDate TEXT,
                    IsActive BOOLEAN DEFAULT 1,
                    PRIMARY KEY (UserID, PlanID),
                    FOREIGN KEY (UserID) REFERENCES User(UserID) ON DELETE CASCADE,
                    FOREIGN KEY (PlanID) REFERENCES SubscriptionPlan(PlanID)
                );
                """
        };

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {

            for (String sql : sqlTables) {
                stmt.execute(sql);
            }

            // Вставляем базовые планы подписки, если их ещё нет
            insertDefaultPlans();

            System.out.println("База данных инициализирована успешно.");
        } catch (SQLException e) {
            System.err.println("Ошибка при инициализации БД: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void tryPreparedStatement(Connection conn, String sql, String value) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, value);
            ps.executeUpdate();
        }
    }

    private static int getArtistId(Connection conn, String name) throws SQLException {
        String sql = "SELECT ArtistID FROM Artist WHERE Name = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    private static void insertDefaultPlans() {
        String sql = "INSERT OR IGNORE INTO SubscriptionPlan (Name, Price, Description) VALUES (?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Free
            pstmt.setString(1, "Free");
            pstmt.setDouble(2, 0.0);
            pstmt.setString(3, "Бесплатный план с рекламой");
            pstmt.addBatch();

            // Premium
            pstmt.setString(1, "Premium");
            pstmt.setDouble(2, 9.99);
            pstmt.setString(3, "Без рекламы, офлайн, лучшее качество");
            pstmt.addBatch();

            // Family
            pstmt.setString(1, "Family");
            pstmt.setDouble(2, 14.99);
            pstmt.setString(3, "До 6 аккаунтов, семейный доступ");
            pstmt.addBatch();

            pstmt.executeBatch();

        } catch (SQLException e) {
            System.err.println("Не удалось вставить планы подписки: " + e.getMessage());
        }
    }

    // ===================== АВТОРИЗАЦИЯ =====================

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
            pstmt.setString(3, hash + ":" + salt); // храним hash:salt в одном поле

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

    // ===================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ДЛЯ ДАЛЬНЕЙШЕГО РАЗВИТИЯ =====================

    public static boolean isPremiumUser(int userId) {
        String sql = """
                SELECT 1 FROM UserSubscription us
                JOIN SubscriptionPlan sp ON us.PlanID = sp.PlanID
                WHERE us.UserID = ? AND us.IsActive = 1 AND sp.Name != 'Free'
                LIMIT 1
                """;
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            return pstmt.executeQuery().next();
        } catch (SQLException e) {
            return false;
        }
    }

    // Пример: получить любимые треки пользователя
    public static ResultSet getLikedTracks(int userId) throws SQLException {
        String sql = """
                SELECT t.TrackID, t.Title, a.Name AS ArtistName, al.Title AS AlbumTitle
                FROM Track t
                JOIN Artist a ON t.ArtistID = a.ArtistID
                LEFT JOIN Album al ON t.AlbumID = al.AlbumID
                JOIN UserLike ul ON t.TrackID = ul.TrackID
                WHERE ul.UserID = ?
                ORDER BY t.Title
                """;
        Connection conn = DriverManager.getConnection(DB_URL);
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, userId);
        return pstmt.executeQuery();
        // Важно: вызывающий код должен закрывать ResultSet и Connection!
    }
}