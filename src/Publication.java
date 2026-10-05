import java.util.Date;

public abstract class Publication {
    private int id;
    private String title;
    private String description;
    private String author;
    private String sourceUrl;
    private String publisher;
    private Date createdAt;
    private Date updatedAt;

    public Publication(int id, String title, String description, String author,
                       String sourceUrl, String publisher) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.author = author;
        this.sourceUrl = sourceUrl;
        this.publisher = publisher;
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void markEdited() {
        this.updatedAt = new Date();
    }
}