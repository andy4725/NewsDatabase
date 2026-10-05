import java.time.LocalDateTime;

public class Event extends Publication {
    private LocalDateTime eventDateTime;
    private String location;

    public Event(int id, String title, String description, String author, String sourceUrl, String publisher, LocalDateTime eventDateTime, String location) {
        super(id, title, description, author, sourceUrl, publisher);
        this.eventDateTime = eventDateTime;
        this.location = location;
    }

    // Add getters and setters for eventDateTime and location.
    public LocalDateTime getEventDateTime() {
        return eventDateTime;
    }

    public void setEventDateTime(LocalDateTime eventDateTime) {
        this.eventDateTime = eventDateTime;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}