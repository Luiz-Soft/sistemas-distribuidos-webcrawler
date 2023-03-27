package cliente;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Scanner;

import search_module.SearchModuleInterface;

// import java.util.Scanner;

public class Cliente {

	private static final int num_of_tries = 5;

	private SearchModuleInterface smi;


	public Cliente() {
		System.out.println("Getting connection ...");
		
		smi = get_server_connection();
		
		if (smi == null) System.out.println("Connection failled");
		else System.out.println("Connection succesful");
	}

	private static SearchModuleInterface get_server_connection() {
		// System.out.println("Getting connection ...");
		
		SearchModuleInterface smi = null;
	
		for (int i = 0; i < num_of_tries; i++){
			
			try {
				smi = (SearchModuleInterface) Naming.lookup("rmi://localhost:1098/search_mod");
				break;
				
			} catch (MalformedURLException | RemoteException | NotBoundException e) {
				// System.out.println("\tRetrying Conection ...");
			}
			
			
			try {
				Thread.sleep(1000);  // wait 1 sec to conect
			} catch (InterruptedException e) {
				e.printStackTrace();
				break;	
			}
		}
		
		// if (smi == null) System.err.println("Connection failled");
		// else System.out.println("Connection succesful");
		
		return smi;
	}

	private void handle_order(String[] params){
		boolean resp;

		try {
			if (params[0].equals("add")){
				resp = smi.querie_url(params[1]);
				if (!resp) System.err.println("Unable to process comand.");

			}else if (params[0].equals("search"))
				smi.search_results();
			
			return;
		} catch (RemoteException | NullPointerException e) {
			smi = get_server_connection();
			if (smi == null) System.err.println("Unable to reach search module.");
			else handle_order(params);
		}

	}

	public void run() {
		Scanner sc = new Scanner(System.in);
		
		while (true) {
			System.out.print(">> ");
			String data = sc.nextLine();
			
			if (data.equals("quit"))
				break;
			
			if (data.equals("list")){
				try {
					for (String iterable_element : Naming.list("rmi://localhost/")) {
						System.out.println(iterable_element);
					}
				} catch (RemoteException | MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				continue;
			}
			
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