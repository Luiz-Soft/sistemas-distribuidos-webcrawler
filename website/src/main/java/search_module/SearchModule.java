package search_module;

import cliente.ClienteInterface;
import indexstoragebarrels.IndexStorageBarrelInterface;
import queue.QueueInterface;
import utils.ProxyStatus;
import utils.SearchResult;
import website.HomeControllerInterface;

import java.io.*;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public class SearchModule extends UnicastRemoteObject implements SearchModuleInterface {

    private static final long serialVersionUID = 1L;
    private static final int num_of_tries = 5;

    private QueueInterface queue;
    private ConcurrentHashMap<String, String> users;
    private ConcurrentLinkedDeque<ClienteInterface> clientes;
    private ConcurrentLinkedDeque<IndexStorageBarrelInterface> ibss;
    private ConcurrentLinkedDeque<HomeControllerInterface> webs;
    private ConcurrentHashMap<String, Integer> pesquisas;

    private String queue_ip_adress;
    private List<String> top10 = new ArrayList<>();

    protected SearchModule(String queue_ip) throws RemoteException {
        super();
        load_from_file();

        queue_ip_adress = queue_ip;
        queue = get_queue_conection();
        clientes = new ConcurrentLinkedDeque<>();
        ibss = new ConcurrentLinkedDeque<>();
		webs = new ConcurrentLinkedDeque<>();
    }


    private QueueInterface get_queue_conection(){
        System.out.println("Getting Queue Connection ...");

        QueueInterface qi = null;

        for (int i = 0; i < num_of_tries; i++) {
            try {
                qi = (QueueInterface) Naming.lookup("rmi://"+queue_ip_adress+"/queue_mod");
                break;

            } catch (MalformedURLException | RemoteException | NotBoundException e) {
                System.out.println("\tRetrying Conection ...");
            }

            try {
                Thread.sleep(1000);  // wait 0.5 sec to cone
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }

        if (qi == null)
            System.out.println("Conection failed");
        else {
            System.out.println("Queue Connection Succesfull.");
            try {
                qi.setSmi(this);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }


        return qi;
    }

    private void load_from_file(){

        users = new ConcurrentHashMap<>();
        pesquisas = new ConcurrentHashMap<>();

        try {
            FileInputStream file = new FileInputStream("smi.ser");
            ObjectInputStream in = new ObjectInputStream(file);
            SearchModule temp = (SearchModule) in.readObject();

            users = temp.users;
            clientes = temp.clientes;

            in.close();
            file.close();
            System.out.println("Loaded by file.");
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("New users set.");
        }
    }

    public void on_end() {
        try {
            FileOutputStream file = new FileOutputStream("smi.ser");
            ObjectOutputStream out = new ObjectOutputStream(file);

            clientes.clear();
            ibss.clear();
			webs.clear();
            queue = null;

            out.writeObject(this);
            out.close();
            file.close();
            System.out.println("Saved on file.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<ProxyStatus> get_queue_status(){
        List<ProxyStatus> resp = new ArrayList<>();

        try {
            resp = queue.get_status();
            return resp;
        } catch (RemoteException | NullPointerException e) {
            queue = get_queue_conection();
        }

        // second try
        try {
            resp = queue.get_status();
        } catch (RemoteException | NullPointerException e) {
            // e.printStackTrace();
        }

        return resp;
    }

    private List<ProxyStatus> get_barrel_status(){
        List<ProxyStatus> resp = new ArrayList<>();

        for (IndexStorageBarrelInterface ibs : ibss) {
            String proxyString = ibs.toString();

            int startIndex = proxyString.indexOf("endpoint:[") + 10;

            proxyString = proxyString.substring(startIndex);
            int endIndex = proxyString.indexOf("]");

            String endpoint = proxyString.substring(0, endIndex); // "endpoint:[127.0.0.1:50087](remote)"

            String[] parts = endpoint.split(":"); // ["endpoint", "127.0.0.1", "50087"](remote)
            String ip = parts[0];
            String port = parts[1];

            resp.add(new ProxyStatus(ip, Integer.parseInt(port)));
        }

        return resp;
    }

    private void update_words_count(List<String> words) {

        for (String word : words) {
            Integer count = pesquisas.get(word);

            if (count == null) {
                pesquisas.put(word, 1);
                count = 1;
            }
            else {
                pesquisas.put(word, count+1);
            }

        }

        List<String> newTop10 = getTop10Words();

        if (!newTop10.equals(top10)) {
            top10 = newTop10;
            try {
                print_status();
            } catch (RemoteException e) {
            }
        }
    }

    private List<String> getTop10Words() {
        PriorityQueue<String> pq = new PriorityQueue<>((w1, w2) -> pesquisas.get(w1) - pesquisas.get(w2));

        for (String word : pesquisas.keySet()) {
            pq.offer(word);
            if (pq.size() > 10) pq.poll();
        }
        List<String> top10 = new ArrayList<>();

        while (!pq.isEmpty()) {
            top10.add(0, pq.poll());
        }
        return top10;
    }

    @Override
    public List<SearchResult> search_results(List<String> terms) {
        System.out.println("Searching ... ");

        update_words_count(terms);

        if (ibss.isEmpty()) return null;

        Random random = new Random();
        IndexStorageBarrelInterface ibs = ibss.getFirst();

        while (!ibss.isEmpty()){
            int randomElement = random.nextInt(ibss.size());
            int i = 0;
            for (IndexStorageBarrelInterface temp : ibss) {
                if (i == randomElement){
                    ibs = temp;
                }
            }

            try {
                return ibs.search(terms);
            } catch (RemoteException e) {
                ibss.remove(ibs);
            }
        }

        if (ibss.isEmpty()) return null;

        return new ArrayList<>();

    }

    @Override
    public boolean querie_url(String url) {

        // first try
        try {
            queue.append_url(url);
            System.out.println(url + " recieved");
            return true;

        } catch (RemoteException | NullPointerException e) {
            queue = get_queue_conection();
        }

        if (queue == null)
            return false;

        // second try
        try {
            queue.append_url(url);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        System.out.println(url + " recieved");

        return true;
    }

    @Override
    public boolean register(String username, String password) throws RemoteException {
        String past_key = users.put(username, password);

        if (past_key != null)
            return false;
        return true;
    }

    @Override
    public int login(String username, String password) throws RemoteException {
        String password_to_check = users.get(username);

        if (password_to_check == null){
            return 0;
        }else if (!password.equals(password_to_check)){
            return -1;
        }

        return 1;
    }

    @Override
    public List<String> probe_url(String url) throws RemoteException {
        if (ibss.isEmpty()) return null;

        List<String> resp = new ArrayList<>();

        Random random = new Random();
        IndexStorageBarrelInterface ibs = ibss.getFirst();

        System.out.println("Porbing ... ");
        while (!ibss.isEmpty()){
            int randomElement = random.nextInt(ibss.size());
            int i = 0;

            for (IndexStorageBarrelInterface temp : ibss) {
                if (i == randomElement){
                    ibs = temp;
                }
            }

            try {
                return ibs.probe(url);
            } catch (RemoteException e) {
                ibss.remove(ibs);
            }
        }

        if (ibss.isEmpty()) return null;

        return resp;
    }

    @Override
    public void register_cliente_obj(ClienteInterface cli) throws RemoteException {
        clientes.add(cli);
    }

    @Override
    public void register_ibs_obj(IndexStorageBarrelInterface ibs) throws RemoteException {
        ibss.add(ibs);
        print_status();
    }

	@Override
	public void register_web_obj(HomeControllerInterface ws) throws RemoteException {
		System.out.println("New web");
		webs.add(ws);
	}

    @Override
    public void print_status() throws RemoteException {
        System.out.println("Printing status");

        ConcurrentLinkedDeque<ClienteInterface> active_clients = new ConcurrentLinkedDeque<>();
        ConcurrentLinkedDeque<HomeControllerInterface> active_webs = new ConcurrentLinkedDeque<>();
        List<List<ProxyStatus>> resp = new ArrayList<>();

        List<ProxyStatus> downloaders = get_queue_status();
        resp.add(downloaders);

        List<ProxyStatus> barrels = get_barrel_status();
        resp.add(barrels);

        for (ClienteInterface client : clientes) {
            try{
                client.print_status(top10, resp);
                active_clients.add(client);
            } catch (RemoteException e){
                //
            }
        }

		for (HomeControllerInterface web: webs) {
			try {
				web.sendMessage(top10, resp);
				active_webs.add(web);
			} catch (RemoteException e){
				// pass
			}
		}

        clientes = active_clients;
		webs = active_webs;
    }

    @Override
    public void ping() throws RemoteException {
    }

    public static void main(String[] args) throws RemoteException {
        Integer port;

        if (args.length == 0 || args[0].equals("--help")){
            System.out.println("Correct usage\n\t search.jar <\"port\"_to_host_on_local> <\"ip:port\"_of_queue>");
            return;
        }else {
            port = Integer.parseInt(args[0]);
        }

        SearchModuleInterface smi = new SearchModule(args[1]);

        LocateRegistry.createRegistry(port).rebind("search_mod", smi);

        System.out.println("Search Module Ready");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ((SearchModule) smi).on_end();
        }));
    }

}
