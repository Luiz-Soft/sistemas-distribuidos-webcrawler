package search_module;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface SearchModuleInterface extends Remote {
	
	String[] search_results() throws RemoteException;
	boolean querie_url(String url) throws RemoteException;
	
	boolean register(String username, String password) throws RemoteException;
	int login(String username, String password) throws RemoteException;

	String[] probe_url(String url) throws RemoteException;
}
