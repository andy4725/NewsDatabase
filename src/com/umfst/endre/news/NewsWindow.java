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

        JPanel progressPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        progressPanel.setPreferredSize(new Dimension(300, 25));
        progressPanel.add(loadingLabel);
        progressPanel.add(progressBar);

        loadingLabel.setVisible(false);
        progressBar.setVisible(false);

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.add(new JLabel("News Database"), BorderLayout.WEST);
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
}