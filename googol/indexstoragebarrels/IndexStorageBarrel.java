package indexstoragebarrels;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import search_module.SearchModuleInterface;

public class IndexStorageBarrel extends UnicastRemoteObject implements IndexStorageBarrelInterface {
    private static final String MULTICAST_GROUP = "224.3.2.1";
    private static final int PORT = 4321;
    private static final int BUFFER_SIZE = 4096;
    private static final String DELIMITER = "|||";

    private HashMap<String, HashSet<String>> index;
    private HashMap<String, HashSet<String>> urlsRelation;
    private HashMap<String, String> urlTitles;
    private HashMap<String, String> urlCitations;
    private MulticastSocket socket;

	private SearchModuleInterface smi;
	private int keepAliveTimer = 10000;

    public IndexStorageBarrel() throws RemoteException {
        index = new HashMap<String, HashSet<String>>();
        urlsRelation = new HashMap<String, HashSet<String>>();
        urlTitles = new HashMap<String, String>();
        urlCitations = new HashMap<String, String>();

		smi = get_smi_conection();
		smi.register_ibs_obj(this);
		keepAlive();
    }

	private SearchModuleInterface get_smi_conection(){
		
		SearchModuleInterface qi = null;
		
		while (qi == null){
			try {
				qi = (SearchModuleInterface) Naming.lookup("rmi://localhost:1098/search_mod");
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

	public void keepAlive() {
		Runnable keepAliveRunnable = () -> {
			while (true) {
				try {
					Thread.sleep(keepAliveTimer);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				try {
					smi.ping();
					continue;
				} catch (RemoteException e) {
					smi = get_smi_conection();
				}

				try {
					smi.register_ibs_obj(this);
				} catch (RemoteException e) {
				}

			}
		};

		Thread assignDownloadersThread = new Thread(keepAliveRunnable);
		assignDownloadersThread.start();
	}


	@Override
    public List<SearchResult> search(List<String> terms) throws RemoteException {
        Set<String> commonUrls = new HashSet<>();

        boolean firstTerm = true;
        for (String term : terms) {
            HashSet<String> urls = index.get(term);

            if (urls == null) {
                return List.of(); // If any term is not found, return an empty list
            }

            if (firstTerm) {
                commonUrls.addAll(urls);
                firstTerm = false;
            } else {
                commonUrls.retainAll(urls);
            }
        }

        List<SearchResult> searchResults = commonUrls.stream().map(url -> {
            String title = urlTitles.get(url);
            String citation = urlCitations.get(url);
            return new SearchResult(title, url, citation);
        }).sorted(Comparator.comparingInt((SearchResult sr) -> { //aqui os resultados da pesquisa são ordenados por ordem de relevancia
            int incomingLinksCount = 0;
            for (HashSet<String> incomingSet : urlsRelation.values()) {
                if (incomingSet.contains(sr.getUrl())) {
                    incomingLinksCount++;
                }
            }
            return incomingLinksCount;
        }).reversed()).collect(Collectors.toList());

        return searchResults;
    }

    public void run() {
        try {
            InetAddress group = InetAddress.getByName(MULTICAST_GROUP);
            socket = new MulticastSocket(PORT);
            socket.joinGroup(group);

            byte[] buffer = new byte[BUFFER_SIZE];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            while (true) {
                socket.receive(packet);

                String data = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                String[] indexData = data.split(Pattern.quote(DELIMITER));

                try {
                    String urlDaPagina = indexData[0];
                    String urlsContidasNaPagina = indexData[1];
                    String tituloDaPagina = indexData[2];
                    String citacaoDaPagina = indexData[3];
                    String[] palavrasDaPagina = indexData[4].split(" ");

                    // Store the title and citation information
                    urlTitles.put(urlDaPagina, tituloDaPagina);
                    urlCitations.put(urlDaPagina, citacaoDaPagina);

                    for (String word : palavrasDaPagina) {
                        if (index.containsKey(word)) {
                            index.get(word).add(urlDaPagina);
                        } else {
                            HashSet<String> urls = new HashSet<>();
                            urls.add(urlDaPagina);
                            index.put(word, urls);
                        }
                    }

                    // Associate urlDaPagina with urlsContidasNaPagina in urlsRelation
                    String[] containedUrls = urlsContidasNaPagina.split(" ");
                    HashSet<String> urlsSet = new HashSet<>(Arrays.asList(containedUrls));
                    urlsRelation.put(urlDaPagina, urlsSet);

                    System.out.println("index and urlsRelation updated "+ urlDaPagina);

                } catch (ArrayIndexOutOfBoundsException e) {
                    System.out.println("Received malformed data. Ignoring...");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                InetAddress group = InetAddress.getByName(MULTICAST_GROUP);
                if (socket != null && !socket.isClosed()) {
                    socket.leaveGroup(group);
                    socket.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Expose a method to send index data
    public void sendIndex(String word, String url) {
        try {
            InetAddress group = InetAddress.getByName(MULTICAST_GROUP);

            String data = word + DELIMITER + url;
            byte[] buffer = data.getBytes(StandardCharsets.UTF_8);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, PORT);
            socket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        IndexStorageBarrel barrel;
		try {
			barrel = new IndexStorageBarrel();
			barrel.run();
		} catch (RemoteException e) {
		}
    }
}
