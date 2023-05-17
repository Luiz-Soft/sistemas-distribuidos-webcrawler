package indexstoragebarrels;


import utils.SearchResult;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;


public interface IndexStorageBarrelInterface extends Remote{
    public List<SearchResult> search(List<String> terms) throws RemoteException;
    public List<String> probe(String url) throws RemoteException;
}
