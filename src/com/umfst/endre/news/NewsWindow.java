package com.umfst.endre.news;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;

import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;

public class NewsWindow extends JFrame {
    private ArrayList<Publication> publications;
    private ArrayList<Publication> visiblePublications;

    private DefaultListModel<String> headlineModel;
    private JList<String> headlineList;
    private JTextArea articleText;
    private JLabel statusLabel;
    private JLabel imageLabel;

    private JTextField searchField;
    private JButton reloadButton;
    private JButton addNewsButton;
    private JButton editNewsButton;
    private JButton deleteNewsButton;

    private Loading loading;
    private NewsCrud newsCrud;

    public NewsWindow() {
        super("News Database");

        publications = new ArrayList<Publication>();
        visiblePublications = new ArrayList<Publication>();

        headlineModel = new DefaultListModel<String>();
        headlineList = new JList<String>(headlineModel);
        articleText = new JTextArea();
        statusLabel = new JLabel("Waiting for news feeds...");
        searchField = new JTextField(18);

        reloadButton = new JButton("Reload");
        addNewsButton = new JButton("Add News");
        editNewsButton = new JButton("Edit News");
        deleteNewsButton = new JButton("Delete News");

        loading = new Loading();
        newsCrud = new NewsCrud(
            this,
            this::getSelectedPublication,
            this::onNewsCreated,
            this::onNewsUpdated,
            this::onNewsDeleted);

        imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setPreferredSize(new Dimension(300, 180));

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
                JSplitPane.HORIZONTAL_SPLIT,
                leftPanel,
                rightPanel);
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

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.add(searchPanel, BorderLayout.EAST);
        headerPanel.add(loading, BorderLayout.SOUTH);

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

        addNewsButton.addActionListener(event -> newsCrud.createNews());
        editNewsButton.addActionListener(event -> newsCrud.updateNews());
        deleteNewsButton.addActionListener(event -> newsCrud.deleteNews());

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
        loading.showLoading();
    }

    public void setLoadingProgress(int value) {
        loading.setLoadingProgress(value);
    }

    public void hideLoading() {
        loading.hideLoading();
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
            HTMLEditorKit.ParserCallback callback =
                    new HTMLEditorKit.ParserCallback() {
                public void handleText(char[] data, int position) {
                    if (text.length() > 0) {
                        text.append(' ');
                    }

                    text.append(data);
                }
            };

            new ParserDelegator().parse(
                    new StringReader(description),
                    callback,
                    true);

        } catch (Exception exception) {
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

            int imageWidth = icon.getIconWidth();
            int imageHeight = icon.getIconHeight();

            if (imageWidth <= 0 || imageHeight <= 0) {
                return;
            }

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
            imageLabel.setIcon(null);
        }
    }

    private void filteredHeadlines() {
        headlineModel.clear();
        visiblePublications.clear();

        String query = searchField.getText().trim().toLowerCase();

        for (Publication publication : publications) {
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

    private Publication getSelectedPublication() {
        int index = headlineList.getSelectedIndex();

        if (index < 0 || index >= visiblePublications.size()) {
            return null;
        }

        return visiblePublications.get(index);
    }

    private void onNewsCreated(News news) {
        publications.add(0, news);
        filteredHeadlines();
        statusLabel.setText("News saved.");
    }

    private void onNewsUpdated(News news) {
        filteredHeadlines();
        statusLabel.setText("News updated.");
    }

    private void onNewsDeleted(News news) {
        publications.remove(news);
        filteredHeadlines();
        statusLabel.setText("News deleted.");
    }
}