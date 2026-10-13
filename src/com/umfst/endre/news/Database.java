package com.umfst.endre.news;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {

    // SQLite stores the database in a file named news.db.
    private static final String URL = "jdbc:sqlite:news.db";

    // Prevents creating Database objects. Its methods are static.
    private Database() {}

    // Opens and returns a connection to the SQLite database.
    // The caller is responsible for closing the connection.
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    // Creates the publications table if it does not already exist.
    public static void initialize() throws SQLException {
        String sql =
            "CREATE TABLE IF NOT EXISTS publications (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "kind TEXT NOT NULL CHECK (kind IN ('NEWS', 'EVENT')), " +
            "title TEXT NOT NULL, " +
            "description TEXT NOT NULL, " +
            "author TEXT NOT NULL, " +
            "source_url TEXT NOT NULL, " +
            "publisher TEXT NOT NULL, " +
            "created_at TEXT NOT NULL, " +
            "updated_at TEXT NOT NULL, " +
            "event_datetime TEXT, " +
            "location TEXT, " +
            "CHECK (" +
            "  (kind = 'NEWS' AND event_datetime IS NULL " +
            "   AND location IS NULL) " +
            "  OR " +
            "  (kind = 'EVENT' AND event_datetime IS NOT NULL " +
            "   AND location IS NOT NULL)" +
            ")" +
            ")";

        // try-with-resources closes both the connection and statement
        // automatically, even if executing the SQL causes an error.
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {

            // Run the table-creation command in the database.
            statement.executeUpdate(sql);
        }
    }
}