import java.awt.BorderLayout;
import java.util.ArrayList;

import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class NewsWindow extends JFrame {
    private ArrayList<Publication> publications;
    private DefaultListModel<String> headlineModel;
    private JList<String> headlineList;
    private JTextArea articleText;
    private JLabel statusLabel;

    public NewsWindow() {
        super("News Database");

        publications = new ArrayList<Publication>();
        headlineModel = new DefaultListModel<String>();
        headlineList = new JList<String>(headlineModel);
        articleText = new JTextArea();
        statusLabel = new JLabel("Waiting for news feeds...");

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
        rightPanel.add(new JLabel("Article"), BorderLayout.NORTH);
        rightPanel.add(new JScrollPane(articleText), BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setDividerLocation(280);
        splitPane.setResizeWeight(0.3);

        add(splitPane, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        headlineList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent event) {
                if(!event.getValueIsAdjusting()) {
                    showSelectedPublication();
                }
            }
        });
    }

    public void setPublications(ArrayList<Publication> loadedPublications) {
        publications = loadedPublications;
        headlineModel.clear();

        for(int i = 0; i < publications.size(); i++) {
            String title = publications.get(i).getTitle();

            if(title.length() == 0) {
                title = "(Untitled)";
            }

            headlineModel.addElement(title);
        }

        if(publications.size() > 0) {
            headlineList.setSelectedIndex(0);
            statusLabel.setText("Loaded" + publications.size() + " items");
        } else {
            statusLabel.setText(("No news items were loaded"));
            statusLabel.setText("No items loaded. Check the console for feed errors.");
        }
    }

    public void setStatus(String message) {
        statusLabel.setText(message);
    }

    private void showSelectedPublication() {
        int index = headlineList.getSelectedIndex();

        if(index < 0 || index >= publications.size()) {
            return;
        }

        Publication publication = publications.get(index);
        StringBuilder text = new StringBuilder();

        text.append(publication.getTitle());
        text.append("\n\nPublisher: ");
        text.append(publication.getPublisher());

        if(publication.getAuthor().length() > 0) {
            text.append("\nAuthor: ");
            text.append(publication.getAuthor());
        }

        if(publication instanceof Event) {
            Event event = (Event) publication;
            text.append("Event date: ");
            text.append(event.getEventDateTime());
            text.append("Location: ");
            text.append(event.getLocation());
        }

        text.append("\n\n");
        text.append(publication.getDescription());

        if(publication.getSourceUrl().length() > 0) {
            text.append("\n\n");
            text.append(publication.getSourceUrl());
        }

        articleText.setText(text.toString());
        articleText.setCaretPosition(0);
    }
}