package com.umfst.endre.news;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class PublicationRepository {

    public int insertToDatabaseIfMissing(Publication publication) throws SQLException {
        String kind;

        if (publication instanceof Event) {
            kind = "EVENT";
        } else if (publication instanceof News) {
            kind = "NEWS";
        } else {
            throw new IllegalArgumentException("Unsupported publication type");
        }

        String sourceUrl = publication.getSourceUrl();

        // Use the source URL to recognize articles already saved.
        if (sourceUrl != null && !sourceUrl.trim().isEmpty()) {
            String findSql =
                    "SELECT id FROM publications " +
                    "WHERE kind = ? AND source_url = ?";

            try (Connection connection = Database.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(findSql)) {

                statement.setString(1, kind);
                statement.setString(2, sourceUrl);

                try (ResultSet result = statement.executeQuery()) {
                    if (result.next()) {
                        int existingId = result.getInt("id");
                        publication.setId(existingId);
                        return existingId;
                    }
                }
            }
        }

        return insert(publication);
    }


    // Insert a news article or event into the publications table.
    // Returns the new database ID and assigns it to the publication object.
    public int insert(Publication publication) throws SQLException {
        String kind;
        String eventDateTime = null;
        String location = null;

        // Identify the publication type and retrieve event-specific values.
        if (publication instanceof Event event) {
            kind = "EVENT";
            eventDateTime = event.getEventDateTime().toString();
            location = event.getLocation();
        } else if (publication instanceof News) {
            kind = "NEWS";
        } else {
            // Reject any Publication subclass this repository does not support.
            throw new IllegalArgumentException("Unsupported publication type");
        }

        // Use placeholders so values can be safely passed to the SQL statement.
        String sql =
            "INSERT INTO publications " +
            "(kind, title, description, author, source_url, publisher, " +
            "created_at, updated_at, event_datetime, location) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        // Automatically close the connection and statement after use.
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            // Set the values for the placeholders in the INSERT statement.
            statement.setString(1, kind);
            statement.setString(2, publication.getTitle());
            statement.setString(3, publication.getDescription());
            statement.setString(4, publication.getAuthor());
            statement.setString(5, publication.getSourceUrl());
            statement.setString(6, publication.getPublisher());
            statement.setString(7, publication.getCreatedAt().toString());
            statement.setString(8, publication.getUpdatedAt().toString());
            statement.setString(9, eventDateTime);
            statement.setString(10, location);

            // Execute the INSERT and store the new row in the database.
            statement.executeUpdate();

            // SQLite's last_insert_rowid() returns the ID just created
            // on this database connection.
            try (Statement idQuery = connection.createStatement();
                 ResultSet result = idQuery.executeQuery(
                         "SELECT last_insert_rowid()")) {

                if (!result.next()) {
                    throw new SQLException(
                            "Could not retrieve the new row ID.");
                }

                int id = result.getInt(1);

                // Keep the in-memory object in sync with the database.
                publication.setId(id);
                return id;
            }
        }
    }

    // Update the saved fields of a news article with the matching ID.
    public void updateNews(News news) throws SQLException {
        String sql =
                "UPDATE publications " +
                "SET title = ?, description = ?, author = ?, source_url = ?, " +
                "publisher = ?, updated_at = ? " +
                "WHERE id = ? AND kind = 'NEWS'";

        try (Connection connection = Database.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            // Set the updated article values and the ID to match.
            statement.setString(1, news.getTitle());
            statement.setString(2, news.getDescription());
            statement.setString(3, news.getAuthor());
            statement.setString(4, news.getSourceUrl());
            statement.setString(5, news.getPublisher());
            statement.setString(6, news.getUpdatedAt().toString());
            statement.setInt(7, news.getId());

            // executeUpdate returns the number of rows changed.
            // Zero means no saved news article matched this ID.
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Saved news article not found.");
            }
        }
    }

    // Delete a saved news article by ID.
    public void deleteNews(int id) throws SQLException {
        String sql =
                "DELETE FROM publications WHERE id = ? AND kind = 'NEWS'";

        try (Connection connection = Database.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, id);

            // If no row was deleted, the requested article was not found.
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Saved news article not found.");
            }
        }
    }

    public void deleteAllFromDatabase() throws SQLException {
        String sql = "DELETE FROM publications";

        try (Connection connection = Database.getConnection();
            Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }
}