package search_module;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;

import queue.QueueInterface;

public class SearchModule extends UnicastRemoteObject implements SearchModuleInterface {

	private static final long serialVersionUID = 1L;
	private static final int num_of_tries = 5;
	
	private QueueInterface queue;

	protected SearchModule() throws RemoteException {
		super();
		queue = null;
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


	public static void main(String[] args) throws RemoteException {
		SearchModuleInterface smi = new SearchModule();
		
		LocateRegistry.createRegistry(1098).rebind("search_mod", smi);

		System.out.println("Search Module Ready");
	}
	
}
