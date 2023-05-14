package indexstoragebarrels;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import search_module.SearchModuleInterface;
import utils.SearchResult;

public class IndexStorageBarrel extends UnicastRemoteObject implements IndexStorageBarrelInterface {
    private static final String MULTICAST_GROUP = "224.3.2.1";
    private static final int PORT = 4321;
    private static final String DELIMITER = "|||";

    private HashMap<String, HashSet<String>> index;
    private HashMap<String, HashSet<String>> urlsRelation;
    private HashMap<String, String> urlTitles;
    private HashMap<String, String> urlCitations;
    private MulticastSocket socket;

    private SearchModuleInterface smi;
    private int keepAliveTimer = 10000;
    private String ip_smi;

    public IndexStorageBarrel(String ip) throws RemoteException {
        load_from_file();
        ip_smi = ip;
        smi = null;
        keepAlive();
    }

    private SearchModuleInterface get_smi_conection(){

        SearchModuleInterface qi = null;

        try {
            qi = (SearchModuleInterface) Naming.lookup("rmi://"+ip_smi+"/search_mod");
        } catch (MalformedURLException | RemoteException | NotBoundException e) {
            System.out.println("Retrying Conection ...");
        }

        if (qi == null) return qi;

        try{
            qi.register_ibs_obj(this);
        } catch (RemoteException e){}

        return qi;
    }

    public void keepAlive() {
        Runnable keepAliveRunnable = () -> {
            while (true) {
                try {
                    smi.ping();
                    continue;
                } catch (RemoteException | NullPointerException e) {
                    smi = get_smi_conection();
                }

                try {
                    Thread.sleep(keepAliveTimer);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        };

        Thread assignDownloadersThread = new Thread(keepAliveRunnable);
        assignDownloadersThread.start();
    }

    public void load_from_file() {

        index = new HashMap<String, HashSet<String>>();
        urlsRelation = new HashMap<String, HashSet<String>>();
        urlTitles = new HashMap<String, String>();
        urlCitations = new HashMap<String, String>();

        try {
            FileInputStream file = new FileInputStream("ibs.ser");
            ObjectInputStream in = new ObjectInputStream(file);
            IndexStorageBarrel temp = (IndexStorageBarrel) in.readObject();

            index = temp.index;
            urlsRelation = temp.urlsRelation;
            urlTitles = temp.urlTitles;
            urlCitations = temp.urlCitations;

            file.close();
            System.out.println("Loaded by file.");
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("New ibs.");
        }

    }

    @Override
    public List<SearchResult> search(List<String> terms) throws RemoteException {
        System.out.println("Searching for " + terms.get(0));

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

    @Override
    public List<String> probe(String url) throws RemoteException {
        System.out.println("Porbing ... ");
        List<String> resp = new ArrayList<>();

        HashSet<String> my_set =  urlsRelation.get(url);
        if (my_set == null) return resp;

        for (String s :  my_set) {
            resp.add(s);
        }

        return resp;
    }


    public void run_helper() {
        Runnable assignDownloadersRunnable = () -> {
            run();
        };

        Thread assignDownloadersThread = new Thread(assignDownloadersRunnable);
        assignDownloadersThread.start();
    }

    public void run() {
        try {
            InetAddress group = InetAddress.getByName(MULTICAST_GROUP);
            socket = new MulticastSocket(PORT);
            socket.joinGroup(group);


            while (true) {
                byte[] buffer = new byte[socket.getReceiveBufferSize()];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
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
                    for (String page_url : containedUrls) {
                        HashSet<String> temp = urlsRelation.get(page_url);

                        if(temp == null){
                            temp = new HashSet<>();
                            urlsRelation.put(page_url, temp);
                        };
                        temp.add(urlDaPagina);
                    }

                    System.out.println("index and urlsRelation updated.");

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

    public void on_end() {
        try {
            FileOutputStream file = new FileOutputStream("ibs.ser");
            ObjectOutputStream out = new ObjectOutputStream(file);

            socket = null;
            smi = null;

            out.writeObject(this);
            out.close();
            file.close();
            System.out.println("Saved on file.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (args.length == 0){
            System.out.println("Correct usage\n\t barrel.jar <\"ip:port\"_of_smi>");
            return;
        }

        final IndexStorageBarrel barrel;

        // localhost:1098
        try {
            barrel = new IndexStorageBarrel(args[0]);
            barrel.run_helper();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                barrel.on_end();
            }));

        } catch (RemoteException e) {}
    }


}
