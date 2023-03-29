package queue;


import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;

import downloader_fake.DownloaderInterface;

class Queue extends UnicastRemoteObject implements QueueInterface{
	private ConcurrentLinkedDeque<DownloaderInterface> free_downloaders;
	private ConcurrentLinkedQueue<String> my_queue;

	protected Queue() throws RemoteException {
		super();
		this.my_queue = new ConcurrentLinkedQueue<>();
		this.free_downloaders = new ConcurrentLinkedDeque<>();
		tryAssignDownloaders();
	}

	public void tryAssignDownloaders() {
		Runnable assignDownloadersRunnable = () -> {
			while (true) {
				if (free_downloaders.isEmpty() || my_queue.isEmpty()){
					try {
						// System.out.println("Waiting");
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					continue;
				}

				String url = my_queue.poll();

				DownloaderInterface downloader = free_downloaders.poll();

				Thread downloaderThread = new Thread(() -> {
					String[] urlsFound;
					try {
						urlsFound = downloader.process_page(url);
						
						if (urlsFound.length > 0) {
							extend_urls(urlsFound);
						}
					
					} catch (RemoteException e) {
						my_queue.add(url);
						return;
					}
					
					free_downloaders.add(downloader);
				});
				
				downloaderThread.start();

			}
		};
	
		Thread assignDownloadersThread = new Thread(assignDownloadersRunnable);
		assignDownloadersThread.start();
	}
	

	@Override
	public void register_downloader(DownloaderInterface downloader) throws RemoteException {
		free_downloaders.add(downloader);
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

	@Override
	public void ping() throws RemoteException {
	}

	public static void main(String[] args) throws RemoteException {
		QueueInterface qi = new Queue();
		
		LocateRegistry.createRegistry(1099).rebind("queue_mod", qi);

		System.out.println("Queue Module Ready");
	}
}