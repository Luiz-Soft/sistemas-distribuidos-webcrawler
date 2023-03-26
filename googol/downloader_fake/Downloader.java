package downloader_fake;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import queue.QueueInterface;

public class Downloader extends UnicastRemoteObject implements DownloaderInterface {

	private QueueInterface queue;


	protected Downloader() throws RemoteException {
		super();
		queue = get_queue_conection();
		queue.register_downloader(this);
	}

	private QueueInterface get_queue_conection(){
		
		QueueInterface qi = null;
		
		while (qi == null){
			try {
				qi = (QueueInterface) Naming.lookup("rmi://localhost/queue_mod");
				return qi;
			
			} catch (MalformedURLException | RemoteException | NotBoundException e) {
				System.out.println("Retrying Conection ...");
			}
			
			try {
				Thread.sleep(500);  // wait 0.5 sec to cone
			} catch (InterruptedException e) {
				e.printStackTrace();
				break;	
			}
		}

		return qi;
	}

	public static void main(String[] args) throws RemoteException {
		new Downloader();
	}
	
}
