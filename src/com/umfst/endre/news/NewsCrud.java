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
    private final JFrame parent;
    private final Supplier<Publication> selectedPublication;
    private final Consumer<News> onCreated;
    private final Consumer<News> onUpdated;
    private final Consumer<News> onDeleted;
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

    public void createNews() {
        NewsForm form = showNewsForm("Add News", null);

        if (form == null) {
            return;
        }

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

    public void updateNews() {
        News news = getSelectedSavedNews();

        if (news == null) {
            return;
        }

        NewsForm form = showNewsForm("Edit News", news);

        if (form == null) {
            return;
        }

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

        if (news.getId() <= 0) {
            showMessage(
                    "Only saved news articles can be edited or deleted.",
                    "Article is not saved");
            return null;
        }

        return news;
    }

    private NewsForm showNewsForm(String dialogTitle, News existingNews) {
        String title = existingNews == null ? "" : existingNews.getTitle();
        String publisher = existingNews == null ? "" : existingNews.getPublisher();
        String website = existingNews == null ? "" : existingNews.getSourceUrl();
        String description =
                existingNews == null ? "" : existingNews.getDescription();

        JTextField titleField = new JTextField(title, 30);
        JTextField publisherField = new JTextField(publisher, 30);
        JTextField websiteField = new JTextField(website, 30);
        JTextArea descriptionArea = new JTextArea(description, 8, 30);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);

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

    private void showDatabaseError(String action, SQLException exception) {
        JOptionPane.showMessageDialog(
                parent,
                "Could not " + action + " the news article: "
                        + exception.getMessage(),
                "Database error",
                JOptionPane.ERROR_MESSAGE);
    }

    private void showMessage(String message, String title) {
        JOptionPane.showMessageDialog(
                parent,
                message,
                title,
                JOptionPane.WARNING_MESSAGE);
    }

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