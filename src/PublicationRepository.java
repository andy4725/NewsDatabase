import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class PublicationRepository {
    public int insert(Publication publication) throws SQLException {
        String kind;
        String eventDateTime = null;
        String location = null;

        if(publication instanceof Event event) {
            kind = "EVENT";
            eventDateTime = event.getEventDateTime().toString();
            location = event.getLocation();
        } else if(publication instanceof News) {
            kind = "NEWS";
        } else {
            throw new IllegalArgumentException("Unsupported publication type");
        }

        String sql = 
            "INSERT INTO publications " +
            "(kind, title, description, author, source_url, publisher, " +
            "created_at, updated_at, event_datetime, location) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
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
            statement.executeUpdate();  
            
            try (Statement idQuery = connection.createStatement();
                 ResultSet result = idQuery.executeQuery(
                 "SELECT last_insert_rowid()")) {
                    if(!result.next()) {
                        throw new SQLException("Could not retrieve the new row ID.");
                    }

                    int id = result.getInt(1);
                    publication.setId(id);
                    return id;
            }
        }
    }
}