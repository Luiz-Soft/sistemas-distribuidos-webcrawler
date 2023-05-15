package cliente;

import utils.ProxyStatus;
import utils.SearchResult;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface ClienteInterface extends Remote {
	void ping() throws RemoteException;
    void print_status(List<String> top10, List<List<ProxyStatus>> info) throws RemoteException;

    boolean handle_add(String url) throws RemoteException;
	List<SearchResult> handle_search(String[] params) throws RemoteException;
	List<String> sudo_handle_probe(String url) throws RemoteException;
}
