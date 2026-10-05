import javax.swing.SwingUtilities;
import java.util.ArrayList;

public class App {
    public static void main(String[] args) throws Exception {
        String[][] sources = {
            { "Info Mures", "https://stiri.infomures.ro/feed/" },
            { "Zi de Zi", "https://www.zi-de-zi.ro/feed/" },
            { "Punctul", "https://punctul.ro/feed/" },
            { "Radio Mures", "https://www.radiomures.ro/feed/" },
            { "KPTV News", "https://news.kptv.ro/feed/" },
            { "laTarguMures", "https://latargumures.ro/feed/" },
            { "Vasarhely.ma", "https://www.vasarhely.ma/feed/" },
            { "Marosvasarhelyi.info", "https://marosvasarhelyi.info/feed/" },
            { "Marosvasarhelyi Radio",
            "https://www.marosvasarhelyiradio.ro/hirek_kategoria/a-nap-hirei/feed/" }
        };

        Database.initialize();

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                NewsWindow window = new NewsWindow();
                window.setVisible(true);
            }
        });

        ArrayList<Publication> publications = new ArrayList<Publication>();
        RssReader reader = new RssReader();

        for(int i = 0; i < sources.length; i++) {
            try {
                ArrayList<Publication> feedPublications = reader.readFeed(sources[i][1], sources[i][0]);
                publications.addAll(feedPublications);
                System.out.println("Loaded feed: " + sources[i][0]);
            } catch(Exception e) {
                System.out.println("Could not load: " + sources[i][0] + ": " + e.getMessage());
            }
        }
    }
}