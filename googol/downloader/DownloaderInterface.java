package downloader;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface DownloaderInterface extends Remote {
	List<String> process_page(String urlS) throws RemoteException;
}
