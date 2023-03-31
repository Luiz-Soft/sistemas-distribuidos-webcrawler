package indexstoragebarrels;


import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import utils.SearchResult;


public interface IndexStorageBarrelInterface extends Remote{
	public List<SearchResult> search(List<String> terms) throws RemoteException;
}
