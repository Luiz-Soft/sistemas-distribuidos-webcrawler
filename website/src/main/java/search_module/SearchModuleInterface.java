package search_module;

import cliente.ClienteInterface;
import indexstoragebarrels.IndexStorageBarrelInterface;
import utils.SearchResult;
import website.HomeControllerInterface;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface SearchModuleInterface extends Remote {

    List<SearchResult> search_results(List<String> terms) throws RemoteException;
    boolean querie_url(String url) throws RemoteException;

    boolean register(String username, String password) throws RemoteException;
    int login(String username, String password) throws RemoteException;

    List<String> probe_url(String url) throws RemoteException;

    void print_status() throws RemoteException;
    void register_cliente_obj(ClienteInterface cli) throws RemoteException;
    void register_ibs_obj(IndexStorageBarrelInterface ibs) throws RemoteException;
    void register_web_obj(HomeControllerInterface ws) throws RemoteException;
    void ping() throws RemoteException;
}
