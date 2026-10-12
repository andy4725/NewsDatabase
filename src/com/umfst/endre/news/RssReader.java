package com.umfst.endre.news;

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
                String imageUrl = getImageUrl(item);
                String eventDateText = getText(item, "eventDateTime");
                String location = getText(item, "location");

                Publication publication;

                if(eventDateText.length() > 0 && location.length() > 0) {
                    Date eventDate = parseEventDate(eventDateText);

                    publication = new Event(0, title, description, author, link, publisher, eventDate, location);
                } else {
                    publication = new News(0, title, description, author, link, publisher);
                }

                publication.setImageUrl(imageUrl);
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

    private String getImageUrl(Element item) {
        // Check RSS enclosure elements.
        NodeList enclosures = item.getElementsByTagName("enclosure");

        for(int i = 0; i < enclosures.getLength(); i++) {
            Element enclosure = (Element) enclosures.item(i);
            String type = enclosure.getAttribute("type");
            String url = enclosure.getAttribute("url");

            if(url.length() > 0 && (type.startsWith("image/") || type.length() == 0)) {
                return url;
            }
        }

        // Check media:content and media:thumbnail elements.
        String[] imageTags = { "media:content", "media:thumbnail" };

        for(int i = 0; i < imageTags.length; i++) {
            NodeList images = item.getElementsByTagName(imageTags[i]);

            if(images.getLength() > 0) {
                Element image = (Element) images.item(0);
                String url = image.getAttribute("url");

                if(url.length() > 0) {
                    return url;
                }
            }
        }

        // Check for an image inside the description.
        String description = getText(item, "description");
        int imgStart = description.indexOf("<img");

        if(imgStart >= 0) {
            int srcStart = description.indexOf("src=", imgStart);

            if(srcStart >= 0) {
                srcStart += 4;

                while(srcStart < description.length() && Character.isWhitespace(description.charAt(srcStart))) {
                    srcStart++;
                }

                if(srcStart < description.length()) {
                    char quote = description.charAt(srcStart);

                    if(quote == '\'' || quote == '"') {
                        int srcEnd = description.indexOf(quote, srcStart + 1);

                        if(srcEnd > srcStart) {
                            return description.substring(srcStart + 1, srcEnd);
                        }
                    }
                }
            }
        }

        return "";
    }

    private Date parseEventDate(String dateText) throws Exception {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        formatter.setLenient(false);
        return formatter.parse(dateText);
    }
}