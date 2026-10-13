package com.umfst.endre.news;

import java.util.ArrayList;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

public class App {

    // Each row contains a publisher name and its RSS feed URL.
    private static final String[][] SOURCES = {
        { "Info Mures", "https://stiri.infomures.ro/feed/" },
        { "Zi de Zi", "https://www.zi-de-zi.ro/feed/" },
        { "Punctul", "https://punctul.ro/feed/" },
        { "Radio Mures", "https://www.radiomures.ro/feed/" },
        { "KPTV News", "https://news.kptv.ro/feed/" },
        { "laTarguMures", "https://latargumures.ro/feed/" },
        { "Vasarhely.ma", "https://www.vasarhely.ma/feed/" },
        { "Marosvasarhelyi.info",
          "https://marosvasarhelyi.info/feed/" },
        { "Marosvasarhelyi Radio",
          "https://www.marosvasarhelyiradio.ro/hirek_kategoria/a-nap-hirei/feed/" }
    };

    public static void main(String[] args) throws Exception {
        // Set up the database before the app starts.
        Database.initialize();

        // Create and show Swing components on Swing's UI thread.
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                final NewsWindow window = new NewsWindow();
                window.setVisible(true);

                // Start loading news after the window is displayed.
                loadNews(window);
            }
        });
    }

    public static void loadNews(final NewsWindow window) {
        // Show the loading indicator and update the status message.
        window.showLoading();
        window.setStatus("Loading news feeds...");

        // SwingWorker loads feeds in the background so the UI stays responsive.
        SwingWorker<ArrayList<Publication>, Void> worker =
                new SwingWorker<ArrayList<Publication>, Void>() {

            // This method runs in the background thread.
            protected ArrayList<Publication> doInBackground()
                    throws Exception {
                ArrayList<Publication> allPublications =
                        new ArrayList<Publication>();
                RssReader reader = new RssReader();

                // Read each RSS feed and collect its publications.
                for (int i = 0; i < SOURCES.length; i++) {
                    try {
                        ArrayList<Publication> feedItems =
                                reader.readFeed(SOURCES[i][1], SOURCES[i][0]);
                        allPublications.addAll(feedItems);
                    } catch (Exception e) {
                        // Report a feed error and continue with the others.
                        System.out.println("Could not load "
                                + SOURCES[i][0] + ": " + e.getMessage());
                    }

                    // Calculate progress as a percentage of feeds processed.
                    final int progress =
                            (int) (((i + 1) * 100.0) / SOURCES.length);

                    // Update the progress indicator on Swing's UI thread.
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            window.setLoadingProgress(progress);
                        }
                    });
                }

                return allPublications;
            }

            // This method runs on Swing's UI thread when loading finishes.
            protected void done() {
                window.hideLoading();

                try {
                    // get() retrieves the list returned by doInBackground().
                    window.setPublications(get());
                    window.setStatus("News loaded.");
                } catch (Exception e) {
                    // Update the UI and log an error if loading failed.
                    window.setStatus("Could not load the news feeds.");
                    System.out.println("News loading error: "
                            + e.getMessage());
                }
            }
        };

        // Start the background loading task.
        worker.execute();
    }
}