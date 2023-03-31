package cliente;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import search_module.SearchModuleInterface;
import utils.ProxyStatus;
import utils.SearchResult;

// import java.util.Scanner;

public class Cliente extends UnicastRemoteObject implements ClienteInterface{

	private static final int num_of_tries = 5;

	private SearchModuleInterface smi;
	private boolean loged_in;
	private boolean recive_status;


	public Cliente() throws RemoteException {
		loged_in = false;
		recive_status = false;
		System.out.println("Getting connection ...");
		
		smi = get_server_connection();
		
		if (smi == null) System.out.println("Connection failled");
		else System.out.println("Connection succesful");
	}

	
	private SearchModuleInterface get_server_connection() {
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
		if (smi != null){
			try {
				smi.register_cliente_obj(this);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

		return smi;
	}

	private void handle_add(String url) throws RemoteException{
		boolean resp = smi.querie_url(url);
		if (!resp) System.err.println("Unable to process comand.");
	}

	private void handle_search(String[] params) throws RemoteException{
		List<String> temp = new ArrayList<>();

		for (String string : params) {
			temp.add(string);
		}

		List<SearchResult> resp = smi.search_results(temp);
		if (resp == null){
			System.err.println("Unable to process comand.");
			return;
		}

		for (SearchResult res : resp) {
			System.out.println(res.toString());
		}
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

	private void handle_status(Scanner sc) throws RemoteException{
		System.out.println("Press Enter to quit.");
		recive_status = true;
		smi.print_status();
		sc.nextLine();

	}

	private void handle_order(String[] params, Scanner sc){

		try {
			switch (params[0]) {
				case "add":
					handle_add(params[1]);
					break;
				case "search":
					handle_search(Arrays.copyOfRange(params, 1, params.length));
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
					break;
				case "status":
					handle_status(sc);
					break;
				default:
					break;
			}			
			return;
			
		} catch (RemoteException | NullPointerException e) {
			smi = get_server_connection();
			if (smi == null) System.err.println("Unable to reach search module.");
			else handle_order(params, sc);
		}

	}

	public void run() {
		Scanner sc = new Scanner(System.in);
		
		while (true) {
			System.out.print(">> ");
			String data = sc.nextLine();
			
			if (data.equals("quit")){
				break;
			}
			
			String[] params = data.split(" ");
			
			handle_order(params, sc);
			
		}

		sc.close();
	}

	@Override
	public void print_status(List<List<ProxyStatus>> info) throws RemoteException {
		if (!recive_status) return;
		System.out.println("#####################");

		System.out.println("Downloaders:");
		for (ProxyStatus downloader : info.get(0)) {
			System.out.println("\t" + downloader.toString());
		}

		System.out.println("Barrels:");
		for (ProxyStatus barrel : info.get(1)) {
			System.out.println("\t" + barrel.toString());
		}
	}


	public static void main(String[] args) {
		Cliente my_client;
		try {
			my_client = new Cliente();
			my_client.run();
			System.exit(0);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		
	}
}