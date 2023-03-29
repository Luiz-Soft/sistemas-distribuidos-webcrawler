
import java.net.URL;
import multicastserver.MulticastServer;
import webcrawler.Downloader;

public class Main {
    public static void main(String[] args) {
        try {
            // downloader parameters
            URL startingUrl = new URL("https://en.wikipedia.org/");
            int numThreads = 1;

            // client and multicast server parameters
            int port = 4321;
            String hostAddress = "224.3.2.1";

            Downloader downloader = new Downloader(startingUrl, numThreads, port, hostAddress);
            downloader.start();

           // MulticastServer multicastServer = new MulticastServer(port, hostAddress);
           //multicastServer.start();

        } catch (Exception e) {
            System.out.println("Error starting downloader: " + e.getMessage());
        }
    }
}
