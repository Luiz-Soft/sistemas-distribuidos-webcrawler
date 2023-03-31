package queue;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import downloader.DownloaderInterface;
import search_module.SearchModuleInterface;
import utils.ProxyStatus;

public interface QueueInterface extends Remote{


	public void register_downloader(DownloaderInterface downloader) throws RemoteException;
	public void append_url(String url) throws RemoteException;
	public void extend_urls(List<String> urls) throws RemoteException;

	public void ping() throws RemoteException;
	public List<ProxyStatus> get_status() throws RemoteException;
	
	public void setSmi(SearchModuleInterface smi) throws RemoteException;
	public void remove_down(DownloaderInterface d) throws RemoteException;
	
}
