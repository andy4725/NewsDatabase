package com.umfst.endre.news;

import java.util.ArrayList;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

public class App {
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
        Database.initialize();

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                final NewsWindow window = new NewsWindow();
                window.setVisible(true);
                loadNews(window);
            }
        });
    }

    public static void loadNews(final NewsWindow window) {
        window.showLoading();
        window.setStatus("Loading news feeds...");

        SwingWorker<ArrayList<Publication>, Void> worker = new SwingWorker<ArrayList<Publication>, Void>() {

        protected ArrayList<Publication> doInBackground() throws Exception {
            ArrayList<Publication> allPublications = new ArrayList<Publication>();
            RssReader reader = new RssReader();

            for (int i = 0; i < SOURCES.length; i++) {
                try {
                    ArrayList<Publication> feedItems = reader.readFeed(SOURCES[i][1], SOURCES[i][0]);
                    allPublications.addAll(feedItems);
                } catch (Exception e) {
                    System.out.println("Could not load " + SOURCES[i][0] + ": " + e.getMessage());
                }

                final int progress = (int) (((i + 1) * 100.0) / SOURCES.length);

                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        window.setLoadingProgress(progress);
                    }
                });
            }

            return allPublications;
        }

        protected void done() {
            window.hideLoading();

            try {
                window.setPublications(get());
                window.setStatus("News loaded.");

            } catch (Exception e) {
                window.setStatus("Could not load the news feeds.");

                System.out.println("News loading error: " + e.getMessage());
            }
        }
    };

    worker.execute();
    }
}