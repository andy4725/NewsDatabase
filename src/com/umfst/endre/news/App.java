package com.umfst.endre.news;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import com.google.gson.Gson;

public class App {
    public static void main(String[] args) throws Exception {
        // Set up the database before the app starts.
        Database.initialize();

        // Create Swing components on Swing's UI thread.
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                final NewsWindow window = new NewsWindow();
                window.setVisible(true);

                // Load the feeds after showing the window.
                loadNews(window);
            }
        });
    }

    public static void loadNews(final NewsWindow window) {
        window.showLoading();
        window.setStatus("Loading news feeds...");

        // SwingWorker loads feeds in the background so the UI stays responsive.
        SwingWorker<ArrayList<Publication>, Void> worker =
                new SwingWorker<ArrayList<Publication>, Void>() {

            protected ArrayList<Publication> doInBackground() throws Exception {
                PublicationRepository repository = new PublicationRepository();

                ArrayList<Publication> allPublications =
                        new ArrayList<Publication>();
                RssReader reader = new RssReader();

                // Read the feed source list from rss-sources.json.
                RssSource[] sources = loadSources();

                // Load each feed and collect its publications.
                for (int i = 0; i < sources.length; i++) {
                    try {
                        ArrayList<Publication> feedItems = reader.readFeed(sources[i].url, sources[i].publisher);

                        for (Publication publication : feedItems) {
                            try {
                                repository.insertToDatabaseIfMissing(publication);
                            } catch (Exception e) {
                                System.out.println("Could not save article: " + e.getMessage());
                            }
                        }

                        allPublications.addAll(feedItems);
                    } catch (Exception e) {
                        System.out.println(
                                "Could not load "
                                + sources[i].publisher + ": "
                                + e.getMessage());
                    }

                    // Update progress based on how many feeds were processed.
                    final int progress =
                            (int) (((i + 1) * 100.0) / sources.length);

                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            window.setLoadingProgress(progress);
                        }
                    });
                }

                return repository.findAll();
            }

            protected void done() {
                window.hideLoading();

                try {
                    window.setPublications(get());
                    window.setStatus("News loaded.");
                } catch (Exception e) {
                    window.setStatus("Could not load the news feeds.");
                    System.out.println(
                            "News loading error: " + e.getMessage());
                }
            }
        };

        worker.execute();
    }

    // Read and parse the JSON array of RSS sources.
    private static RssSource[] loadSources() throws Exception {
        Gson gson = new Gson();

        try (Reader reader = Files.newBufferedReader(
                Paths.get("rss_sources.json"),
                StandardCharsets.UTF_8)) {

            RssSource[] sources = gson.fromJson(reader, RssSource[].class);

            // Treat an empty JSON file or JSON null as an empty source list.
            return sources == null ? new RssSource[0] : sources;
        }
    }

    // Gson fills these fields from each object in the JSON array.
    private static class RssSource {
        private String publisher;
        private String url;
    }
}