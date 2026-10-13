package com.umfst.endre.news;

import java.awt.GridLayout;
import java.sql.SQLException;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class NewsCrud {
    // The window used as the parent for dialogs.
    private final JFrame parent;

    // Provides the article currently selected in the main window.
    private final Supplier<Publication> selectedPublication;

    // Callbacks that let the main window update its display after a change.
    private final Consumer<News> onCreated;
    private final Consumer<News> onUpdated;
    private final Consumer<News> onDeleted;

    // Handles saving, updating, and deleting news in the database.
    private final PublicationRepository repository;

    public NewsCrud(
            JFrame parent,
            Supplier<Publication> selectedPublication,
            Consumer<News> onCreated,
            Consumer<News> onUpdated,
            Consumer<News> onDeleted) {
        this.parent = parent;
        this.selectedPublication = selectedPublication;
        this.onCreated = onCreated;
        this.onUpdated = onUpdated;
        this.onDeleted = onDeleted;
        this.repository = new PublicationRepository();
    }

    // Ask for article details, save a new article, and notify the main window.
    public void createNews() {
        NewsForm form = showNewsForm("Add News", null);

        // A null form means the user cancelled or did not provide required data.
        if (form == null) {
            return;
        }

        // ID 0 indicates this is a new article without a database ID yet.
        News news = new News(
                0,
                form.title,
                form.description,
                "",
                form.website,
                form.publisher);

        try {
            repository.insert(news);
            onCreated.accept(news);
        } catch (SQLException exception) {
            showDatabaseError("save", exception);
        }
    }

    // Edit the selected saved news article and persist the changes.
    public void updateNews() {
        News news = getSelectedSavedNews();

        if (news == null) {
            return;
        }

        // Pre-fill the form with the article's current values.
        NewsForm form = showNewsForm("Edit News", news);

        if (form == null) {
            return;
        }

        // Keep the existing ID and author while using the edited form values.
        News updatedNews = new News(
                news.getId(),
                form.title,
                form.description,
                news.getAuthor(),
                form.website,
                form.publisher);
        updatedNews.markEdited();

        try {
            repository.updateNews(updatedNews);

            // Update the in-memory object so the UI reflects the saved values.
            news.setTitle(updatedNews.getTitle());
            news.setDescription(updatedNews.getDescription());
            news.setSourceUrl(updatedNews.getSourceUrl());
            news.setPublisher(updatedNews.getPublisher());
            news.setUpdatedAt(updatedNews.getUpdatedAt());

            onUpdated.accept(news);
        } catch (SQLException exception) {
            showDatabaseError("update", exception);
        }
    }

    // Confirm the deletion, remove the selected article, and notify the UI.
    public void deleteNews() {
        News news = getSelectedSavedNews();

        if (news == null) {
            return;
        }

        int choice = JOptionPane.showConfirmDialog(
                parent,
                "Delete \"" + news.getTitle() + "\"?",
                "Delete News",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        // Do nothing unless the user confirms.
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            repository.deleteNews(news.getId());
            onDeleted.accept(news);
        } catch (SQLException exception) {
            showDatabaseError("delete", exception);
        }
    }

    // Validate that the selected publication is a saved News object.
    private News getSelectedSavedNews() {
        Publication publication = selectedPublication.get();

        if (publication == null) {
            showMessage("Select a news article first.", "No article selected");
            return null;
        }

        if (!(publication instanceof News)) {
            showMessage(
                    "Only news articles can be edited or deleted.",
                    "Unsupported item");
            return null;
        }

        News news = (News) publication;

        // A positive ID indicates the article was saved in the database.
        if (news.getId() <= 0) {
            showMessage(
                    "Only saved news articles can be edited or deleted.",
                    "Article is not saved");
            return null;
        }

        return news;
    }

    // Display the add/edit form and return its values if the user accepts.
    private NewsForm showNewsForm(String dialogTitle, News existingNews) {
        // Use blank values for a new article, or existing values when editing.
        String title = existingNews == null ? "" : existingNews.getTitle();
        String publisher = existingNews == null
                ? "" : existingNews.getPublisher();
        String website = existingNews == null
                ? "" : existingNews.getSourceUrl();
        String description = existingNews == null
                ? "" : existingNews.getDescription();

        JTextField titleField = new JTextField(title, 30);
        JTextField publisherField = new JTextField(publisher, 30);
        JTextField websiteField = new JTextField(website, 30);

        // Allow long descriptions to wrap and scroll.
        JTextArea descriptionArea = new JTextArea(description, 8, 30);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);

        // Arrange labels and input fields vertically in the dialog.
        JPanel form = new JPanel(new GridLayout(0, 1, 0, 6));
        form.add(new JLabel("Title"));
        form.add(titleField);
        form.add(new JLabel("Publisher"));
        form.add(publisherField);
        form.add(new JLabel("Website URL"));
        form.add(websiteField);
        form.add(new JLabel("Description"));
        form.add(new JScrollPane(descriptionArea));

        int choice = JOptionPane.showConfirmDialog(
                parent,
                form,
                dialogTitle,
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        // Return null if the user cancels the dialog.
        if (choice != JOptionPane.OK_OPTION) {
            return null;
        }

        // Remove extra whitespace from the required fields.
        String enteredTitle = titleField.getText().trim();
        String enteredPublisher = publisherField.getText().trim();

        // Title and publisher must be provided.
        if (enteredTitle.isEmpty() || enteredPublisher.isEmpty()) {
            showMessage(
                    "Title and publisher are required.",
                    "Missing information");
            return null;
        }

        // Bundle the form values for the create or update method.
        return new NewsForm(
                enteredTitle,
                enteredPublisher,
                websiteField.getText().trim(),
                descriptionArea.getText().trim());
    }

    // Show an error dialog when a database operation fails.
    private void showDatabaseError(String action, SQLException exception) {
        JOptionPane.showMessageDialog(
                parent,
                "Could not " + action + " the news article: "
                        + exception.getMessage(),
                "Database error",
                JOptionPane.ERROR_MESSAGE);
    }

    // Show a warning dialog for invalid selections or missing information.
    private void showMessage(String message, String title) {
        JOptionPane.showMessageDialog(
                parent,
                message,
                title,
                JOptionPane.WARNING_MESSAGE);
    }

    // Holds the values entered in the add/edit form.
    private static class NewsForm {
        private final String title;
        private final String publisher;
        private final String website;
        private final String description;

        private NewsForm(
                String title,
                String publisher,
                String website,
                String description) {
            this.title = title;
            this.publisher = publisher;
            this.website = website;
            this.description = description;
        }
    }
}