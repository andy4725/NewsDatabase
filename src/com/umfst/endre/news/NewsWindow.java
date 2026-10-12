package com.umfst.endre.news;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Dimension;
import java.awt.Image;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;

import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.JProgressBar;
import javax.swing.ImageIcon;
import javax.swing.SwingConstants;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;
import java.awt.GridLayout;
import java.sql.SQLException;
import javax.swing.JOptionPane;

public class NewsWindow extends JFrame {
    private ArrayList<Publication> publications;
    private DefaultListModel<String> headlineModel;
    private JList<String> headlineList;
    private JTextArea articleText;
    private JLabel statusLabel;
    private ArrayList<Publication> visiblePublications;
    private JTextField searchField;
    private JButton reloadButton;
    private JProgressBar progressBar;
    private JLabel loadingLabel;
    private JLabel imageLabel;
    private JButton addNewsButton;
    private JButton editNewsButton;
    private JButton deleteNewsButton;

    public NewsWindow() {
        super("News Database");

        publications = new ArrayList<Publication>();
        headlineModel = new DefaultListModel<String>();
        headlineList = new JList<String>(headlineModel);
        articleText = new JTextArea();
        statusLabel = new JLabel("Waiting for news feeds...");
        visiblePublications = new ArrayList<Publication>();
        searchField = new JTextField(18);
        reloadButton = new JButton("Reload");
        loadingLabel = new JLabel("Loading...");
        addNewsButton = new JButton("Add News");
        editNewsButton = new JButton("Edit News");
        deleteNewsButton = new JButton("Delete News");

        imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setPreferredSize(new Dimension(300, 180));

        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setIndeterminate(true);
        progressBar.setPreferredSize(new Dimension(150, 20));

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 550);
        setLocationRelativeTo(null);

        headlineList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        articleText.setEditable(false);
        articleText.setLineWrap(true);
        articleText.setWrapStyleWord(true);

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(new JLabel("Headlines"), BorderLayout.NORTH);
        leftPanel.add(new JScrollPane(headlineList), BorderLayout.CENTER);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(imageLabel, BorderLayout.NORTH);

        JLabel articleLabel = new JLabel("Article");
        JPanel articleLabelPanel = new JPanel(new BorderLayout());
        articleLabelPanel.add(articleLabel, BorderLayout.NORTH);
        rightPanel.add(articleLabelPanel, BorderLayout.SOUTH);

        rightPanel.add(new JScrollPane(articleText), BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setDividerLocation(280);
        splitPane.setResizeWeight(0.3);

        JButton searchButton = new JButton("Search");

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        searchPanel.add(new JLabel("Search headlines:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        searchPanel.add(reloadButton);
        searchPanel.add(addNewsButton);
        searchPanel.add(editNewsButton);
        searchPanel.add(deleteNewsButton);

        JPanel progressPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        progressPanel.setPreferredSize(new Dimension(300, 25));
        progressPanel.add(loadingLabel);
        progressPanel.add(progressBar);

        loadingLabel.setVisible(false);
        progressBar.setVisible(false);

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.add(searchPanel, BorderLayout.EAST);
        headerPanel.add(progressPanel, BorderLayout.SOUTH);

        add(splitPane, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
        add(headerPanel, BorderLayout.NORTH);

        headlineList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent event) {
                if (!event.getValueIsAdjusting()) {
                    showSelectedPublication();
                }
            }
        });

        ActionListener searchAction = new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                filteredHeadlines();
            }
        };

        searchButton.addActionListener(searchAction);
        searchField.addActionListener(searchAction);
        addNewsButton.addActionListener(event -> addNews());
        editNewsButton.addActionListener(event -> updateNews());
        deleteNewsButton.addActionListener(event -> deleteNews());

        reloadButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                reloadNews();
            }
        });
    }

    public void setPublications(ArrayList<Publication> loadedPublications) {
        publications = loadedPublications;
        filteredHeadlines();
    }

    public void setStatus(String message) {
        statusLabel.setText(message);
    }

    public void showLoading() {
        progressBar.setValue(0);
        progressBar.setMaximum(100);
        progressBar.setIndeterminate(false);
        progressBar.setStringPainted(false);

        loadingLabel.setVisible(true);
        progressBar.setVisible(true);

        progressBar.getParent().revalidate();
        progressBar.getParent().repaint();
    }

    public void setLoadingProgress(int value) {
        progressBar.setValue(value);
    }

    public void hideLoading() {
        loadingLabel.setVisible(false);
        progressBar.setVisible(false);

        progressBar.getParent().revalidate();
        progressBar.getParent().repaint();
    }

    public void reloadNews() {
        showLoading();
        setStatus("Reloading news...");
        App.loadNews(this);
    }

    private void showSelectedPublication() {
        int index = headlineList.getSelectedIndex();

        if (index < 0 || index >= visiblePublications.size()) {
            return;
        }

        Publication publication = visiblePublications.get(index);
        StringBuilder text = new StringBuilder();

        text.append(publication.getTitle());
        text.append("\n\nPublisher: ");
        text.append(publication.getPublisher());

        text.append("\n\n");
        text.append(cleanDescription(publication.getDescription()));

        if (publication.getSourceUrl() != null
                && publication.getSourceUrl().length() > 0) {
            text.append("\n\nWebsite: ");
            text.append(publication.getSourceUrl());
        }

        articleText.setText(text.toString());
        articleText.setCaretPosition(0);

        showPublicationImage(publication);
    }

    private String cleanDescription(String description) {
        if (description == null || description.length() == 0) {
            return "";
        }

        final StringBuilder text = new StringBuilder();

        try {
            HTMLEditorKit.ParserCallback callback = new HTMLEditorKit.ParserCallback() {
                public void handleText(char[] data, int position) {
                    if (text.length() > 0) {
                        text.append(' ');
                    }

                    text.append(data);
                }
            };

            new ParserDelegator().parse(
                    new StringReader(description), callback, true);

        } catch (Exception e) {
            return description.replaceAll("<[^>]*>", " ").trim();
        }

        return text.toString().replaceAll("\\s+", " ").trim();
    }

    private void showPublicationImage(Publication publication) {
        imageLabel.setIcon(null);

        String urlString = publication.getImageUrl();

        if (urlString == null || urlString.length() == 0) {
            return;
        }

        try {
            URL url = new URL(urlString);
            ImageIcon icon = new ImageIcon(url);

            int imgWidth = icon.getIconWidth();
            int imgHeight = icon.getIconHeight();

            if (imgWidth <= 0 || imgHeight <= 0) {
                return;
            }

            int maxWidth = 300;
            int maxHeight = 180;

            double widthRatio = (double) maxWidth / imgWidth;
            double heightRatio = (double) maxHeight / imgHeight;
            double scaleFactor = Math.min(widthRatio, heightRatio);

            if (scaleFactor < 1.0) {
                int newWidth = (int) (imgWidth * scaleFactor);
                int newHeight = (int) (imgHeight * scaleFactor);

                Image scaledImg = icon.getImage().getScaledInstance(
                        newWidth, newHeight, Image.SCALE_SMOOTH);

                icon = new ImageIcon(scaledImg);
            }

            imageLabel.setIcon(icon);

        } catch (Exception e) {
            imageLabel.setIcon(null);
        }
    }

    private void filteredHeadlines() {
        headlineModel.clear();
        visiblePublications.clear();

        String query = searchField.getText().trim().toLowerCase();

        for (int i = 0; i < publications.size(); i++) {
            Publication publication = publications.get(i);
            String title = publication.getTitle();

            if (title == null) {
                title = "";
            }

            if (query.length() == 0
                    || title.toLowerCase().indexOf(query) >= 0) {
                visiblePublications.add(publication);

                if (title.length() == 0) {
                    headlineModel.addElement("(Untitled)");
                } else {
                    headlineModel.addElement(title);
                }
            }
        }

        if (visiblePublications.size() > 0) {
            headlineList.setSelectedIndex(0);
            statusLabel.setText(
                    "Showing " + visiblePublications.size()
                    + " of " + publications.size() + " items.");
        } else {
            articleText.setText("No matching headlines.");
            imageLabel.setIcon(null);
            statusLabel.setText("No matching headlines.");
        }
    }

    private void addNews() {
        JTextField titleField = new JTextField(10);
        JTextField publisherField = new JTextField(10);
        JTextField websiteField = new JTextField(10);
        JTextArea descriptionArea = new JTextArea(8, 30);
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
                this,
                form,
                "Add News",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (choice != JOptionPane.OK_OPTION) {
            return;
        }

        String title = titleField.getText().trim();
        String publisher = publisherField.getText().trim();

        if (title.isEmpty() || publisher.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Title and publisher are required.",
                    "Missing information",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        News news = new News(
                0,
                title,
                descriptionArea.getText().trim(),
                "",
                websiteField.getText().trim(),
                publisher);

        try {
            new PublicationRepository().insert(news);
            publications.add(0, news);
            filteredHeadlines();
            headlineList.setSelectedIndex(0);
            statusLabel.setText("News saved.");

        } catch (SQLException exception) {
            JOptionPane.showMessageDialog(
                    this,
                    "Could not save the news article: " + exception.getMessage(),
                    "Database error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private News getSelectedSavedNews() {
        int index = headlineList.getSelectedIndex();

        if (index < 0 || index >= visiblePublications.size()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Select a news article first.",
                    "No article selected",
                    JOptionPane.WARNING_MESSAGE);
            return null;
        }

        Publication publication = visiblePublications.get(index);

        if (!(publication instanceof News)) {
            JOptionPane.showMessageDialog(
                    this,
                    "Only news articles can be edited or deleted.",
                    "Unsupported item",
                    JOptionPane.WARNING_MESSAGE);
            return null;
        }

        News news = (News) publication;

        if (news.getId() <= 0) {
            JOptionPane.showMessageDialog(
                    this,
                    "Only manually saved news articles can be edited or deleted.",
                    "Article is not saved",
                    JOptionPane.WARNING_MESSAGE);
            return null;
        }

        return news;
    }

    private void updateNews() {
        News news = getSelectedSavedNews();

        if (news == null) {
            return;
        }

        JTextField titleField = new JTextField(news.getTitle(), 30);
        JTextField publisherField = new JTextField(news.getPublisher(), 30);
        JTextField websiteField = new JTextField(news.getSourceUrl(), 30);
        JTextArea descriptionArea = new JTextArea(news.getDescription(), 8, 30);
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
                this,
                form,
                "Edit News",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (choice != JOptionPane.OK_OPTION) {
            return;
        }

        String title = titleField.getText().trim();
        String publisher = publisherField.getText().trim();

        if (title.isEmpty() || publisher.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Title and publisher are required.",
                    "Missing information",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        News updatedNews = new News(
                news.getId(),
                title,
                descriptionArea.getText().trim(),
                news.getAuthor(),
                websiteField.getText().trim(),
                publisher);
        updatedNews.markEdited();

        try {
            new PublicationRepository().updateNews(updatedNews);

            news.setTitle(updatedNews.getTitle());
            news.setDescription(updatedNews.getDescription());
            news.setSourceUrl(updatedNews.getSourceUrl());
            news.setPublisher(updatedNews.getPublisher());
            news.setUpdatedAt(updatedNews.getUpdatedAt());

            filteredHeadlines();
            statusLabel.setText("News updated.");

        } catch (SQLException exception) {
            JOptionPane.showMessageDialog(
                    this,
                    "Could not update the news article: " + exception.getMessage(),
                    "Database error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteNews() {
        News news = getSelectedSavedNews();

        if (news == null) {
            return;
        }

        int choice = JOptionPane.showConfirmDialog(
                this,
                "Delete \"" + news.getTitle() + "\"?",
                "Delete News",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            new PublicationRepository().deleteNews(news.getId());
            publications.remove(news);
            filteredHeadlines();
            statusLabel.setText("News deleted.");

        } catch (SQLException exception) {
            JOptionPane.showMessageDialog(
                    this,
                    "Could not delete the news article: " + exception.getMessage(),
                    "Database error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}