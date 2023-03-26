package queue;


import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;

import downloader_fake.DownloaderInterface;

class Queue extends UnicastRemoteObject implements QueueInterface{
	private ConcurrentLinkedDeque<DownloaderInterface> free_dowloaders;
	private ConcurrentLinkedQueue<String> my_queue;

	protected Queue() throws RemoteException {
		super();
		this.my_queue = new ConcurrentLinkedQueue<>();
		this.free_dowloaders = new ConcurrentLinkedDeque<>();
	}

	@Override
	public void register_downloader(DownloaderInterface downloader) throws RemoteException {
		free_dowloaders.add(downloader);
		System.out.println("New downloader registed.");
	}

	@Override
	public void append_url(String url) throws RemoteException {
		System.out.println(url + " recived");
		my_queue.add(url);
	}

	@Override
	public void extend_urls(String[] urls) throws RemoteException {
		for (String url : urls) {
			my_queue.add(url);
		}
	}

	public static void main(String[] args) throws RemoteException {
		QueueInterface qi = new Queue();
		
		try {
			LocateRegistry.createRegistry(1099).rebind("queue_mod", qi);
		} catch (RemoteException e) {
			LocateRegistry.getRegistry(1099).rebind("queue_mod", qi);
		}

		System.out.println("Queue Module Ready");
	}
}