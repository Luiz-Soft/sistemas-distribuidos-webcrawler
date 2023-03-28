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
	private boolean loged_in;


	public Cliente() {
		loged_in = false;
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

	private void handle_add(String url) throws RemoteException{
		boolean resp = smi.querie_url(url);
		if (!resp) System.err.println("Unable to process comand.");
	}

	private void handle_search() throws RemoteException{
		smi.search_results();
	}

	private void handle_register(String username, String password) throws RemoteException{
		boolean resp = smi.register(username, password);

		if (!resp)
			System.out.println("User already exists");
		else{
			System.out.println("Register successful");
			loged_in = true;
			System.out.println("Logged in");
		}
	}

	private void handle_login(String username, String password) throws RemoteException{
		int resp = smi.login(username, password);
		
		if (resp == 1){
			System.out.println("Logged in");
			loged_in = true;
		}else if (resp == 0){
			System.out.println("User not found");
		}else if (resp == -1){
			System.out.println("Wrong password");
		}
	}

	private void handle_probe(String url) throws RemoteException{
		if (!loged_in){
			System.out.println("Need to login");
			return;
		}
		smi.probe_url(url);
	}

	
	private void handle_order(String[] params){

		try {
			switch (params[0]) {
				case "add":
					handle_add(params[1]);
					break;
				case "search":
					handle_search();
					break;
				case "register":
					handle_register(params[1], params[2]);
					break;
				case "login":
					handle_login(params[1], params[2]);
					break;
				case "probe":
					handle_probe(params[1]);
					break;
				case "logout":
					loged_in = false;
					System.out.println("Loged out");
				default:
					break;
			}			
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