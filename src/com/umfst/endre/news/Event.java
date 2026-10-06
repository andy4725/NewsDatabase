package com.umfst.endre.news;
import java.util.Date;

public class Event extends Publication {
    private Date eventDateTime;
    private String location;

    public Event(int id, String title, String description, String author, String sourceUrl, String publisher, Date eventDateTime, String location) {
        super(id, title, description, author, sourceUrl, publisher);
        this.eventDateTime = eventDateTime;
        this.location = location;
    }

    // Add getters and setters for eventDateTime and location.
    public Date getEventDateTime() {
        return eventDateTime;
    }

    public void setEventDateTime(Date eventDateTime) {
        this.eventDateTime = eventDateTime;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}