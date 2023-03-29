package downloader_fake;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import queue.QueueInterface;

public class Downloader extends UnicastRemoteObject implements DownloaderInterface {

	private QueueInterface queue;
	private int keepAliveTimer = 10000;


	protected Downloader() throws RemoteException {
		super();
		queue = get_queue_conection();
		queue.register_downloader(this);
		keepAlive();
	}

	private QueueInterface get_queue_conection(){
		
		QueueInterface qi = null;
		
		while (qi == null){
			try {
				qi = (QueueInterface) Naming.lookup("rmi://localhost/queue_mod");
				break;
			
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

	@Override
	public String[] process_page(String url) throws RemoteException {
		String[] ola = {};
		
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		System.out.println(url + " processed");
		return ola;
	}

	public void keepAlive() {
		Runnable keepAliveRunnable = () -> {
			while (true) {
				try {
					Thread.sleep(keepAliveTimer);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}


				try {
					queue.ping();
					continue;
				} catch (RemoteException e) {
					queue = get_queue_conection();
				}

				try {
					queue.register_downloader(this);
				} catch (RemoteException e) {
				}

			}
		};
	
		Thread assignDownloadersThread = new Thread(keepAliveRunnable);
		assignDownloadersThread.start();
	}

	public static void main(String[] args) throws RemoteException {
		new Downloader();
	}

}
