package downloader_fake;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface DownloaderInterface extends Remote {
	String[] process_page(String url) throws RemoteException;
}
