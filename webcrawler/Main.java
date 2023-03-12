package webcrawler;

import java.net.URL;
import multicastserver.MulticastServer;

public class Main {
    public static void main(String[] args) {
        try {
            URL startingUrl = new URL("https://en.wikipedia.org/");
            int numThreads = 100;
            int port = 4321;
            Downloader downloader = new Downloader(startingUrl, numThreads, port);
            downloader.start();

            MulticastServer multicastServer = new MulticastServer(port);
            multicastServer.start();
        } catch (Exception e) {
            System.out.println("Error starting downloader: " + e.getMessage());
        }
    }
}
