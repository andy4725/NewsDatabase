package com.umfst.endre.news;

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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Dimension;

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
        String title = existingNews == null ? "" : existingNews.getTitle();
        String publisher = existingNews == null
                ? "" : existingNews.getPublisher();
        String website = existingNews == null
                ? "" : existingNews.getSourceUrl();
        String description = existingNews == null
                ? "" : existingNews.getDescription();

        // Shorter fields and a smaller description box reduce dialog size.
        JTextField titleField = new JTextField(title, 22);
        JTextField publisherField = new JTextField(publisher, 22);
        JTextField websiteField = new JTextField(website, 22);

        JTextArea descriptionArea = new JTextArea(description, 5, 22);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);

        JScrollPane descriptionScroll = new JScrollPane(descriptionArea);
        descriptionScroll.setPreferredSize(new Dimension(320, 100));

        // Put labels in the left column and inputs in the right column.
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(5, 5, 5, 5);
        constraints.anchor = GridBagConstraints.WEST;

        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 0;
        constraints.fill = GridBagConstraints.NONE;
        form.add(new JLabel("Title:"), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        form.add(titleField, constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.weightx = 0;
        constraints.fill = GridBagConstraints.NONE;
        form.add(new JLabel("Publisher:"), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        form.add(publisherField, constraints);

        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.weightx = 0;
        constraints.fill = GridBagConstraints.NONE;
        form.add(new JLabel("Website URL:"), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        form.add(websiteField, constraints);

        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.weightx = 0;
        constraints.weighty = 0;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        form.add(new JLabel("Description:"), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.fill = GridBagConstraints.BOTH;
        form.add(descriptionScroll, constraints);

        int choice = JOptionPane.showConfirmDialog(
                parent,
                form,
                dialogTitle,
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (choice != JOptionPane.OK_OPTION) {
            return null;
        }

        String enteredTitle = titleField.getText().trim();
        String enteredPublisher = publisherField.getText().trim();

        if (enteredTitle.isEmpty() || enteredPublisher.isEmpty()) {
            showMessage(
                    "Title and publisher are required.",
                    "Missing information");
            return null;
        }

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