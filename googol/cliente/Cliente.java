package cliente;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Scanner;

import search_module.SearchModuleInterface;

// import java.util.Scanner;

public class Cliente {

	private SearchModuleInterface smi;


	public Cliente() {
		smi = get_server_connection();
	}

	private static SearchModuleInterface get_server_connection() {
		System.out.println("Getting connection ...");
		
		SearchModuleInterface smi = null;
	
		while (true){
			
			try {
				smi = (SearchModuleInterface) Naming.lookup("rmi://localhost/search_mod");
				System.out.println("Connection succesful");
				return smi;
			
			} catch (MalformedURLException | RemoteException | NotBoundException e) {
				System.out.println("\tRetrying Conection ...");
			}
			
			
			try {
				Thread.sleep(1000);  // wait 1 sec to conect
			} catch (InterruptedException e) {
				e.printStackTrace();
				break;	
			}
		}

		return null;
	}

	private void handle_order(String[] params){
		
		try {
			if (params[0].equals("add"))
			smi.querie_url(params[1]);
		else if (params[0].equals("search"))
			smi.search_results();
			
			return;
			
		} catch (RemoteException e) {
			System.out.println("Connection lost, enable to complete operation");
			return;
		}

	}

	public void run() {
		Scanner sc = new Scanner(System.in);
		
		while (true) {
			System.out.print(">> ");
			String data = sc.nextLine();
			
			if (data.equals("quit"))
				break;
			
			String[] params = data.split(" ");
			
			handle_order(params);
			
		}

		sc.close();
	}


	public static void main(String[] args) {
		Cliente my_client = new Cliente();
		my_client.run();
	}

}