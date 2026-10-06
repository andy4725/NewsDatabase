import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class RssReader {
    public ArrayList<Publication> readFeed(String feedUrl, String publisher) throws Exception {
        ArrayList<Publication> publications = new ArrayList<Publication>();

        HttpURLConnection connection = null;
        InputStream input = null;

        try {
            URL url = new URL(feedUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestMethod("GET");

            int statusCode = connection.getResponseCode();

            if(statusCode != HttpURLConnection.HTTP_OK) {
                throw new Exception("The feed returned HTTP status " + statusCode);
            }

            input = connection.getInputStream();

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(input);

            NodeList items = document.getElementsByTagName("item");

            for(int i = 0; i < items.getLength(); i++) {
                Element item = (Element) items.item(i);

                String title = getText(item, "title");
                String description = getText(item, "description");
                String author = getText(item, "author");
                String link = getText(item, "link");
                String eventDateText = getText(item, "eventDateTime");
                String location = getText(item, "location");

                Publication publication;

                if(eventDateText.length() > 0 && location.length() > 0) {
                    Date eventDate = parseEventDate(eventDateText);

                    publication = new Event(0, title, description, author, link, publisher, eventDate, location);
                } else {
                    publication = new News(0, title, description, author, link, publisher);
                }

                publications.add(publication);
            }
        } finally {
            if(input != null) {
                input.close();
            }

            if(connection != null) {
                connection.disconnect();
            }
        }

        return publications;
    }

    private String getText(Element element, String tagName) {
        NodeList nodes = element.getElementsByTagName(tagName);

        if(nodes.getLength() == 0) {
            return "";
        }

        return nodes.item(0).getTextContent().trim();
    }

    private Date parseEventDate(String dateText) throws Exception {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        formatter.setLenient(false);
        return formatter.parse(dateText);
    }
}