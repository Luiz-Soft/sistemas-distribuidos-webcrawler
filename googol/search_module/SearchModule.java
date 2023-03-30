package search_module;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentHashMap;

import queue.QueueInterface;

public class SearchModule extends UnicastRemoteObject implements SearchModuleInterface {

	private static final long serialVersionUID = 1L;
	private static final int num_of_tries = 5;
	
	private QueueInterface queue;
	private ConcurrentHashMap<String, String> users;

	protected SearchModule() throws RemoteException {
		super();
		queue = null;
		users = init_my_hashmap();
	}

	private QueueInterface get_queue_conection(){
		System.out.println("Getting Queue Connection ...");

		QueueInterface qi = null;
		
		for (int i = 0; i < num_of_tries; i++) {
			try {
				System.out.println(Naming.list("rmi://localhost/"));
				qi = (QueueInterface) Naming.lookup("rmi://localhost/queue_mod");
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
		else System.out.println("Queue Connection Succesfull.");

		return qi;
	}

	@SuppressWarnings("unchecked")
	private ConcurrentHashMap<String, String> init_my_hashmap(){
		
		ConcurrentHashMap<String, String> resp = new ConcurrentHashMap<>();

		try {
			FileInputStream file = new FileInputStream("users.ser");
			ObjectInputStream in = new ObjectInputStream(file);
			resp = (ConcurrentHashMap<String, String>) in.readObject();
			
			file.close();
			System.out.println("Loaded by file.");
		} catch (IOException | ClassNotFoundException e) {
			System.out.println("New users set.");
		}

		return resp;
	}

	public void on_end() {
		try {
			FileOutputStream file = new FileOutputStream("users.ser");
			ObjectOutputStream out = new ObjectOutputStream(file);
			
			out.writeObject(users);
			out.close();
			file.close();
			System.out.println("Saved on file.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	

	@Override
	public String[] search_results() {
		throw new UnsupportedOperationException("Unimplemented method 'search_results'");
	}

	@Override
	public boolean querie_url(String url) {

		// first try
		try {
			queue.append_url(url);
			System.out.println(url + " recived");
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

		System.out.println(url + " recived");

		return true;
	}

	@Override
	public boolean register(String username, String password) {
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
	public String[] probe_url(String url) throws RemoteException {
		String[] ola = {};
		
		return ola;
	}


	public static void main(String[] args) throws RemoteException {
		SearchModuleInterface smi = new SearchModule();
		
		LocateRegistry.createRegistry(1098).rebind("search_mod", smi);

		System.out.println("Search Module Ready");

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			((SearchModule) smi).on_end();
		}));
	}

}
