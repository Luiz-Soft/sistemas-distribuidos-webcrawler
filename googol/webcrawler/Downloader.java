package webcrawler;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.MulticastSocket;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.nio.charset.StandardCharsets;

public class Downloader {
  private final ExecutorService executorService;
  private final int numThreads;
  private final MulticastSocket socket;
  private final InetAddress group;
  private final int PORT;
  private final String HOST;;
  private static final String DELIMITER = "|||";

  public Downloader(URL startingUrl, int numThreads, int port, String host) throws Exception {
    executorService = Executors.newFixedThreadPool(numThreads);
    this.numThreads = numThreads;
    this.HOST = host;
    socket = new MulticastSocket();
    group = InetAddress.getByName(HOST);
    this.PORT = port;

    for (int i = 0; i < numThreads; i++) {
      executorService.submit(new DownloadTask(startingUrl));
    }
  }

  public void start() {
    executorService.shutdown();
    try {
      executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    } catch (InterruptedException e) {
      System.out.println("Downloader was interrupted");
      Thread.currentThread().interrupt();
    } finally {
      socket.close();
    }
  }

  private void sendIndex(String url, String content, String links) {
    try {
      String delimiter = "|||";
      String combinedString = url + delimiter + links + delimiter + content;
      byte[] buffer = combinedString.getBytes(StandardCharsets.UTF_8);
      DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, PORT);
      socket.send(packet);
      System.out.println("Sent to index! ");
    } catch (Exception e) {
      System.out.println("Error sending index: " + e.getMessage());
    }
  }

  private class DownloadTask implements Runnable {
    private final ConcurrentLinkedQueue<URL> urlsToVisit;
    private final Set<URL> urlsVisited;

    public DownloadTask(URL startingUrl) {
      urlsToVisit = new ConcurrentLinkedQueue<>();
      urlsVisited = new HashSet<>();
      urlsToVisit.add(startingUrl);
    }

    private void processPage(URL url) {
      try {
        String protocol = url.getProtocol();
        if (!protocol.equals("http") && !protocol.equals("https")) {
          System.out.println("Skipping URL with invalid protocol: " + url);
          return;
        }
        Document doc = Jsoup.connect(url.toString()).get();
        Elements links = doc.select("a[href]");
        for (Element link : links) {
          String href = link.attr("href");
          try {
            URL newUrl = new URL(url, href);
            if (!urlsVisited.contains(newUrl) && !urlsToVisit.contains(newUrl)) {
              urlsToVisit.add(newUrl);
            }
          } catch (MalformedURLException e) {
            // Ignore malformed URLs
          }
        }
        System.out.println(url + " downloaded.");
        // Update index with downloaded content
        String indexContent = doc.body().text();
        sendIndex(url.toString(), indexContent, links.toString());
      } catch (Exception e) {
        System.out.println("Error processing URL " + url + ": " + e.getMessage());
      }
    }

    @Override
    public void run() {
      while (!urlsToVisit.isEmpty() && !Thread.currentThread().isInterrupted()) {
        URL url = urlsToVisit.poll();
        if (url != null && !urlsVisited.contains(url)) {
          urlsVisited.add(url);
          processPage(url);
        }
      }
    }
  }

}
