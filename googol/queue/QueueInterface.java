package queue;

import java.rmi.Remote;
import java.rmi.RemoteException;

import downloader_fake.DownloaderInterface;

public interface QueueInterface extends Remote{


	public void register_downloader(DownloaderInterface downloader) throws RemoteException;
	public void append_url(String url) throws RemoteException;
	public void extend_urls(String[] urls) throws RemoteException;

	public void ping() throws RemoteException;
	
}
