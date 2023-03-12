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

public class Downloader {
  private final ConcurrentLinkedQueue<URL> urlsToVisit;
  private final Set<URL> urlsVisited;
  private final ExecutorService executorService;
  private final int numThreads;
  private final MulticastSocket socket;
  private final InetAddress group;
  private final int PORT;

  public Downloader(URL startingUrl, int numThreads, int port) throws Exception {
    urlsToVisit = new ConcurrentLinkedQueue<>();
    urlsVisited = new HashSet<>();
    urlsToVisit.add(startingUrl);
    executorService = Executors.newFixedThreadPool(numThreads);
    this.numThreads = numThreads;
    socket = new MulticastSocket();
    group = InetAddress.getByName("224.3.2.1");
    this.PORT = port;
  }

  public void start() {
    for (int i = 0; i < numThreads; i++) {
      executorService.submit(new DownloadTask());
    }
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

  private void sendIndex(String content) {
    try {
      byte[] buffer = content.getBytes();
      DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, PORT);
      socket.send(packet);
    } catch (Exception e) {
      System.out.println("Error sending index: " + e.getMessage());
    }
  }

  private class DownloadTask implements Runnable {
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
        sendIndex(indexContent);
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
