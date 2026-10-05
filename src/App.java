public class App {
    public static void main(String[] args) throws Exception {
        Database.initialize();

        News sample = new News(
            0,
            "Test headline",
            "Test description",
            "Test author",
            "https://example.com",
            "Test publisher"
        );

        int id = new PublicationRepository().insert(sample);
        System.out.println("Saved news with ID: " + id);
    }
}
