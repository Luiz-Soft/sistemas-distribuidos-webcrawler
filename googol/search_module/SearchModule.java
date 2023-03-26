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
	private QueueInterface queue;

	protected SearchModule() throws RemoteException {
		super();
		queue = get_queue_conection();
	}

	private QueueInterface get_queue_conection(){
		
		QueueInterface qi = null;
		
		while (qi == null){
			try {
				qi = (QueueInterface) Naming.lookup("rmi://localhost/queue_mod");
				return qi;
			
			} catch (MalformedURLException | RemoteException | NotBoundException e) {
				System.out.println("Retrying Conection ...");
			}
			
			try {
				Thread.sleep(1000);  // wait 0.5 sec to cone
			} catch (InterruptedException e) {
				e.printStackTrace();
				break;	
			}
		}

		return qi;
	}


	@Override
	public String[] search_results() {
		throw new UnsupportedOperationException("Unimplemented method 'search_results'");
	}

	@Override
	public boolean querie_url(String url) {
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
		
		try {
			LocateRegistry.getRegistry(1099).rebind("search_mod", smi);
		} catch (RemoteException e) {
			LocateRegistry.createRegistry(1099).rebind("search_mod", smi);
		}

		System.out.println("Search Module Ready");
	}
	
}
