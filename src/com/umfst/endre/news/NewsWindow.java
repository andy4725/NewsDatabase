package com.umfst.endre.news;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;

public class NewsWindow extends JFrame {

    // All publications loaded from the feeds or saved in the database.
    private ArrayList<Publication> publications;

    // Publications currently shown in the list after applying the search.
    private ArrayList<Publication> visiblePublications;

    // The model stores the headline strings displayed by the JList.
    private DefaultListModel<String> headlineModel;
    private JList<String> headlineList;

    // Displays the selected article's title, publisher, description, and URL.
    private JTextPane articleText;

    // Displays status and the selected article's image.
    private JLabel statusLabel;
    private JLabel imageLabel;

    // Search field and buttons used to interact with the app.
    private javax.swing.JTextField searchField;
    private JButton reloadButton;
    private JButton addNewsButton;
    private JButton editNewsButton;
    private JButton deleteNewsButton;
    private JButton clearDatabaseButton;
    private Set<Publication> readPublications = new HashSet<Publication>();

    // Helper objects for loading feeds and managing news CRUD operations.
    private Loading loading;
    private NewsCrud newsCrud;

    public NewsWindow() {
        super("News Database");

        // Initialize the lists that hold all and currently visible publications.
        publications = new ArrayList<Publication>();
        visiblePublications = new ArrayList<Publication>();

        // Create the list model and the Swing components.
        headlineModel = new DefaultListModel<String>();
        headlineList = new JList<String>(headlineModel);
        articleText = new JTextPane();
        statusLabel = new JLabel("Waiting for news feeds...");
        searchField = new javax.swing.JTextField(18);

        reloadButton = new JButton("Reload");
        addNewsButton = new JButton("Add");
        editNewsButton = new JButton("Edit");
        deleteNewsButton = new JButton("Delete");
        clearDatabaseButton = new JButton("Clear Database");

        loading = new Loading();

        // Give NewsCrud access to the current selection and UI update callbacks.
        newsCrud = new NewsCrud(
            this,
            this::getSelectedPublication,
            this::onNewsCreated,
            this::onNewsUpdated,
            this::onNewsDeleted);

        // Reserve space for the selected publication's image.
        imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setPreferredSize(new Dimension(300, 180));

        // Configure the main application window.
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 550);
        setLocationRelativeTo(null);

        // Allow only one headline to be selected at a time.
        headlineList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Let rows use the height they need when a headline wraps to multiple lines.
        headlineList.setFixedCellHeight(-1);

        // Customize how each headline row is drawn in the list.
        headlineList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {

                // Apply the standard Swing label setup first.
                super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);

                // Use HTML to wrap long headline text within the available width.
                setText("<html><div style='width:190px;'>"
                        + escapeHtml(String.valueOf(value))
                        + "</div></html>");

                // Set the headline font and add space around the text.
                boolean isRead = index >= 0
                    && index < visiblePublications.size()
                    && readPublications.contains(visiblePublications.get(index));

            setFont(list.getFont().deriveFont(
                    isRead ? Font.PLAIN : Font.BOLD, 14f));
                setBorder(javax.swing.BorderFactory.createEmptyBorder(
                        9, 10, 9, 10));

                // Use different colors for the selected row and other rows.
                if (isSelected) {
                    setBackground(new Color(220, 232, 250));
                    setForeground(new Color(30, 50, 80));
                } else {
                    setBackground(index % 2 == 0
                            ? Color.WHITE : new Color(246, 248, 251));
                    setForeground(new Color(40, 45, 55));
                }

                // Return this styled label so Swing can draw the row.
                return this;
            }
        });

        // Make article text read-only and set its default font.
        articleText.setEditable(false);
        articleText.setFont(new Font("SansSerif", Font.PLAIN, 14));

        // Build the left panel that contains the headline list.
        JPanel leftPanel = new JPanel(new BorderLayout());

        JLabel headlinesLabel = new JLabel("Headlines");
        headlinesLabel.setBorder(
                javax.swing.BorderFactory.createEmptyBorder(8, 10, 8, 10));
        headlinesLabel.setFont(
                headlinesLabel.getFont().deriveFont(Font.BOLD, 16f));

        leftPanel.add(headlinesLabel, BorderLayout.NORTH);
        leftPanel.add(new JScrollPane(headlineList), BorderLayout.CENTER);

        // Build the right panel that contains the image and article text.
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(imageLabel, BorderLayout.NORTH);

        JLabel articleLabel = new JLabel("Article");
        articleLabel.setFont(articleLabel.getFont().deriveFont(Font.BOLD, 16f));
        articleLabel.setBorder(
                javax.swing.BorderFactory.createEmptyBorder(8, 10, 8, 10));

        JPanel articleLabelPanel = new JPanel(new BorderLayout());
        articleLabelPanel.add(articleLabel, BorderLayout.NORTH);
        rightPanel.add(articleLabelPanel, BorderLayout.SOUTH);
        rightPanel.add(new JScrollPane(articleText), BorderLayout.CENTER);

        // Place the headline list and article panel beside each other.
        JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                leftPanel,
                rightPanel);
        splitPane.setDividerLocation(280);
        splitPane.setResizeWeight(0.3);

        // Create the search button and the top control panel.
        JButton searchButton = new JButton("Search");

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        searchPanel.add(reloadButton);
        searchPanel.add(addNewsButton);
        searchPanel.add(editNewsButton);
        searchPanel.add(deleteNewsButton);
        searchPanel.add(clearDatabaseButton);

        // Place the controls and loading indicator in the window header.
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.add(searchPanel, BorderLayout.EAST);
        headerPanel.add(loading, BorderLayout.SOUTH);

        // Add the main panels to the window.
        add(splitPane, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
        add(headerPanel, BorderLayout.NORTH);

        // Show the selected publication when the user selects a headline.
        headlineList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent event) {
                if (!event.getValueIsAdjusting()) {
                    showSelectedPublication();
                }
            }
        });

        // Filter the headlines when the user clicks Search or presses Enter.
        ActionListener searchAction = new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                filteredHeadlines();
            }
        };

        searchButton.addActionListener(searchAction);
        searchField.addActionListener(searchAction);

        // Connect the CRUD buttons to their corresponding operations.
        addNewsButton.addActionListener(event -> newsCrud.createNews());
        editNewsButton.addActionListener(event -> newsCrud.updateNews());
        deleteNewsButton.addActionListener(event -> newsCrud.deleteNews());
        clearDatabaseButton.addActionListener(event -> confirmAndClearDatabase());

        // Reload all configured RSS feeds when Reload is clicked.
        reloadButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                reloadNews();
            }
        });
    }

    // Receive loaded publications and refresh the headline list.
    public void setPublications(ArrayList<Publication> loadedPublications) {
        publications = loadedPublications;
        filteredHeadlines();
    }

    // Update the status message at the bottom of the window.
    public void setStatus(String message) {
        statusLabel.setText(message);
    }

    // Show the loading indicator.
    public void showLoading() {
        loading.showLoading();
    }

    // Update the loading progress bar.
    public void setLoadingProgress(int value) {
        loading.setLoadingProgress(value);
    }

    // Hide the loading indicator.
    public void hideLoading() {
        loading.hideLoading();
    }

    // Start reloading the RSS feeds and update the status.
    public void reloadNews() {
        showLoading();
        setStatus("Reloading news...");
        App.loadNews(this);
    }

    // Display the currently selected publication in the article panel.
    private void showSelectedPublication() {
        int index = headlineList.getSelectedIndex();

        // Stop if the selection does not match a visible publication.
        if (index < 0 || index >= visiblePublications.size()) {
            return;
        }

        Publication publication = visiblePublications.get(index);
        readPublications.add(publication);
        headlineList.repaint();
        StyledDocument doc = articleText.getStyledDocument();

        // Define text styles for the title, field labels, and article body.
        SimpleAttributeSet titleStyle = new SimpleAttributeSet();
        StyleConstants.setBold(titleStyle, true);
        StyleConstants.setFontSize(titleStyle, 20);

        SimpleAttributeSet labelStyle = new SimpleAttributeSet();
        StyleConstants.setBold(labelStyle, true);

        SimpleAttributeSet bodyStyle = new SimpleAttributeSet();
        StyleConstants.setFontSize(bodyStyle, 14);

        try {
            // Remove the previous article's contents.
            doc.remove(0, doc.getLength());

            // Use a fallback label if the publication has no title.
            String title = publication.getTitle();
            if (title == null || title.length() == 0) {
                title = "(Untitled)";
            }

            // Add the title in bold, followed by the publisher and description.
            doc.insertString(doc.getLength(), title + "\n\n", titleStyle);

            doc.insertString(doc.getLength(), "Publisher: ", labelStyle);
            doc.insertString(doc.getLength(),
                    safeText(publication.getPublisher()) + "\n\n",
                    bodyStyle);

            doc.insertString(doc.getLength(),
                    cleanDescription(publication.getDescription()) + "\n\n",
                    bodyStyle);

            // Add the website label and URL if a URL is available.
            String website = publication.getSourceUrl();
            if (website != null && website.length() > 0) {
                doc.insertString(doc.getLength(), "Website: ", labelStyle);
                doc.insertString(doc.getLength(), website, bodyStyle);
            }

            // Scroll the article panel back to the beginning.
            articleText.setCaretPosition(0);
        } catch (javax.swing.text.BadLocationException exception) {
            exception.printStackTrace();
        }

        // Show the selected publication's image, if it has one.
        showPublicationImage(publication);
    }

    // Replace a null string with an empty string for display.
    private String safeText(String text) {
        return text == null ? "" : text;
    }

    // Escape HTML-sensitive characters so headlines display as plain text.
    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    // Convert an HTML description into readable plain text.
    private String cleanDescription(String description) {
        if (description == null || description.length() == 0) {
            return "";
        }

        final StringBuilder text = new StringBuilder();

        try {
            // The parser callback collects text while ignoring HTML tags.
            HTMLEditorKit.ParserCallback callback =
                    new HTMLEditorKit.ParserCallback() {
                public void handleText(char[] data, int position) {
                    if (text.length() > 0) {
                        text.append(' ');
                    }

                    text.append(data);
                }
            };

            // Parse the description and pass each text section to the callback.
            new ParserDelegator().parse(
                    new StringReader(description),
                    callback,
                    true);

        } catch (Exception exception) {
            // If parsing fails, remove tags with a simple fallback.
            return description.replaceAll("<[^>]*>", " ").trim();
        }

        // Replace repeated whitespace with single spaces.
        return text.toString().replaceAll("\\s+", " ").trim();
    }

    // Load and display the selected publication's image when available.
    private void showPublicationImage(Publication publication) {
        // Clear the previous image before trying to load another.
        imageLabel.setIcon(null);

        String urlString = publication.getImageUrl();

        // Stop if this publication has no image URL.
        if (urlString == null || urlString.length() == 0) {
            return;
        }

        try {
            URL url = URI.create(urlString).toURL();
            ImageIcon icon = new ImageIcon(url);

            int imageWidth = icon.getIconWidth();
            int imageHeight = icon.getIconHeight();

            // Ignore images that did not load with valid dimensions.
            if (imageWidth <= 0 || imageHeight <= 0) {
                return;
            }

            // Scale the image down if it exceeds the display area.
            int maxWidth = 300;
            int maxHeight = 180;

            double widthRatio = (double) maxWidth / imageWidth;
            double heightRatio = (double) maxHeight / imageHeight;
            double scaleFactor = Math.min(widthRatio, heightRatio);

            if (scaleFactor < 1.0) {
                int newWidth = (int) (imageWidth * scaleFactor);
                int newHeight = (int) (imageHeight * scaleFactor);

                Image scaledImage = icon.getImage().getScaledInstance(
                        newWidth,
                        newHeight,
                        Image.SCALE_SMOOTH);

                icon = new ImageIcon(scaledImage);
            }

            imageLabel.setIcon(icon);

        } catch (Exception exception) {
            // Clear the image if the URL is invalid or loading fails.
            imageLabel.setIcon(null);
        }
    }

    // Rebuild the headline list using the current search text.
    private void filteredHeadlines() {
        headlineModel.clear();
        visiblePublications.clear();

        String query = searchField.getText().trim().toLowerCase();

        // Add publications whose titles match the search text.
        for (Publication publication : publications) {
            String title = publication.getTitle();

            // Treat a missing title as an empty string for searching.
            if (title == null) {
                title = "";
            }

            if (query.length() == 0
                    || title.toLowerCase().indexOf(query) >= 0) {
                visiblePublications.add(publication);

                // Display a fallback label when a publication has no title.
                if (title.length() == 0) {
                    headlineModel.addElement("(Untitled)");
                } else {
                    headlineModel.addElement(title);
                }
            }
        }

        // Select the first matching item and update the status message.
        if (visiblePublications.size() > 0) {
            headlineList.setSelectedIndex(0);
            statusLabel.setText(
                    "Showing " + visiblePublications.size()
                    + " of " + publications.size() + " items.");
        } else {
            // Clear the article and image when there are no search results.
            articleText.setText("No matching headlines.");
            imageLabel.setIcon(null);
            statusLabel.setText("No matching headlines.");
        }
    }

    private void confirmAndClearDatabase() {
        int choice = javax.swing.JOptionPane.showConfirmDialog(
                this,
                "This will permanently delete all saved publications. Continue?",
                "Clear Database",
                javax.swing.JOptionPane.YES_NO_OPTION,
                javax.swing.JOptionPane.WARNING_MESSAGE);

        if (choice != javax.swing.JOptionPane.YES_OPTION) {
            return;
        }

        try {
            new PublicationRepository().deleteAllFromDatabase();

            // Remove the cleared records from the current display too.
            publications.clear();
            searchField.setText("");
            filteredHeadlines();

            statusLabel.setText("Database cleared.");
        } catch (java.sql.SQLException exception) {
            javax.swing.JOptionPane.showMessageDialog(
                    this,
                    "Could not clear the database: " + exception.getMessage(),
                    "Database error",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

    // Return the publication corresponding to the selected headline.
    private Publication getSelectedPublication() {
        int index = headlineList.getSelectedIndex();

        if (index < 0 || index >= visiblePublications.size()) {
            return null;
        }

        return visiblePublications.get(index);
    }

    // Add a newly created article to the in-memory list and refresh the display.
    private void onNewsCreated(News news) {
        publications.add(0, news);
        filteredHeadlines();
        statusLabel.setText("News saved.");
    }

    // Refresh the list after an article has been updated.
    private void onNewsUpdated(News news) {
        filteredHeadlines();
        statusLabel.setText("News updated.");
    }

    // Remove a deleted article from the in-memory list and refresh the display.
    private void onNewsDeleted(News news) {
        publications.remove(news);
        filteredHeadlines();
        statusLabel.setText("News deleted.");
    }
}