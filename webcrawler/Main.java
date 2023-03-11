package webcrawler;

import java.net.URL;

public class Main {
    public static void main(String[] args) {
        try {
            URL startingUrl = new URL("https://en.wikipedia.org/");
            int numThreads = 400;
            Downloader downloader = new Downloader(startingUrl, numThreads);
            downloader.start();
        } catch (Exception e) {
            System.out.println("Error starting downloader: " + e.getMessage());
        }
    }
}
