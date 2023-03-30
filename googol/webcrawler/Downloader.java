package webcrawler;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.MulticastSocket;
import java.net.URL;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import queue.QueueInterface;

import java.nio.charset.StandardCharsets;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

public class Downloader extends UnicastRemoteObject implements DownloaderInterface {
	private QueueInterface queue;
	private int keepAliveTimer = 10000;

	private final MulticastSocket socket;
	private final InetAddress group;
	private final int PORT;
	private final String HOST;
	private static final String DELIMITER = "|||";

	public Downloader(int port, String host) throws IOException {
		super();
		queue = get_queue_conection();
		queue.register_downloader(this);
		keepAlive();

		this.HOST = host;
		this.PORT = port;
		socket = new MulticastSocket();
		group = InetAddress.getByName(HOST);

	}

  	private QueueInterface get_queue_conection(){
		
		QueueInterface qi = null;
		
		while (qi == null){
			try {
				qi = (QueueInterface) Naming.lookup("rmi://localhost/queue_mod");
				break;
			
			} catch (MalformedURLException | RemoteException | NotBoundException e) {
				System.out.println("Retrying Conection ...");
			}
			
			try {
				Thread.sleep(500);  // wait 0.5 sec to cone
			} catch (InterruptedException e) {
				e.printStackTrace();
				break;	
			}
		}
		
		return qi;
	}

	private void sendIndex(String url, String title, String citation, String content, String links) {
		try {
		String combinedString = url + DELIMITER + links + DELIMITER + title + DELIMITER + citation + DELIMITER + content;
		byte[] buffer = combinedString.getBytes(StandardCharsets.UTF_8);
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, PORT);
		socket.send(packet);
		System.out.println("Sent to index! ");
		} catch (Exception e) {
		System.out.println("Error sending index: " + e.getMessage());
		}
	}

  	@Override
  	public List<String> process_page(String urlS) throws RemoteException {
		List<String> urlsFound = new ArrayList<>();
		URL url;
		
		
		try {
			url = new URL(urlS);
			String protocol = url.getProtocol();
			if (!protocol.equals("http") && !protocol.equals("https")) {
				System.out.println("Skipping URL with invalid protocol: " + url);
				return urlsFound;
			}

			Document doc = Jsoup.connect(url.toString()).get();
			Elements links = doc.select("a[href]");
			StringBuilder sb = new StringBuilder();
			
			for (Element link : links) {
				String href = link.attr("href");
				try {
					URL newUrl = new URL(url, href);
					sb.append(newUrl.toString()).append(" ");
					urlsFound.add(newUrl.toString());
				} catch (MalformedURLException e) {
				// Ignore malformed URLs
				}
			}

			System.out.println(url + " downloaded.");

			// Get the title and citation of the page
			String pageTitle = doc.title();
			String pageCitation = doc.select("p").first().text();

			// Update index with downloaded content
			String indexContent = doc.body().text();
			String linksAsString = sb.toString();
			sendIndex(url.toString(), pageTitle, pageCitation, indexContent, linksAsString);

		} catch (Exception e) {
			// System.out.println("Error processing URL " + urlS + ": " + e.getMessage());
		}

		return urlsFound;
  	}

  	public void keepAlive() {
		Runnable keepAliveRunnable = () -> {
			while (true) {
				try {
					Thread.sleep(keepAliveTimer);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}


				try {
					queue.ping();
					continue;
				} catch (RemoteException e) {
					queue = get_queue_conection();
				}

				try {
					queue.register_downloader(this);
				} catch (RemoteException e) {
				}

			}
		};

		Thread assignDownloadersThread = new Thread(keepAliveRunnable);
		assignDownloadersThread.start();
	}

	public void on_end() {
		try {
			queue.remove_down(this);
		} catch (RemoteException e) {
			// no queue, dont need to remove myself
		}
	}

	public static void main(String[] args) {
		try {
			Downloader d =  new Downloader(4321, "224.3.2.1");
			
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				d.on_end();
			}));

		} catch (IOException e) {
			e.printStackTrace();
		}

  	}

}
