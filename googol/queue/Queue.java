package queue;


import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;

import downloader.DownloaderInterface;
import search_module.SearchModuleInterface;
import utils.ProxyStatus;

class Queue extends UnicastRemoteObject implements QueueInterface{
	private ConcurrentLinkedDeque<DownloaderInterface> free_downloaders;
	private ConcurrentLinkedDeque<DownloaderInterface> downloaders;
	private ConcurrentLinkedQueue<String> my_queue;
	private SearchModuleInterface smi;

	protected Queue() throws RemoteException {
		super();
		this.my_queue = init_my_queue();
		this.free_downloaders = new ConcurrentLinkedDeque<>();
		this.downloaders = new ConcurrentLinkedDeque<>();
		tryAssignDownloaders();
	}

	
	private void tryAssignDownloaders() {
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
					List<String> new_urls;
					try {
						new_urls = downloader.process_page(url);
						
						if (new_urls.size() > 0) {
							extend_urls(new_urls);
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
	
	@SuppressWarnings("unchecked")
	private ConcurrentLinkedQueue<String> init_my_queue(){
		
		ConcurrentLinkedQueue<String> resp = new ConcurrentLinkedQueue<>();

		try {
			FileInputStream file = new FileInputStream("my_queue.ser");
			ObjectInputStream in = new ObjectInputStream(file);
			resp = (ConcurrentLinkedQueue<String>) in.readObject();
			
			file.close();
			System.out.println("Loaded by file.");
		} catch (IOException | ClassNotFoundException e) {
			System.out.println("New queue.");
		}

		return resp;
	}

	public void on_end() {
		try {
			FileOutputStream file = new FileOutputStream("my_queue.ser");
			ObjectOutputStream out = new ObjectOutputStream(file);
			
			out.writeObject(my_queue);
			out.close();
			file.close();
			System.out.println("Saved on file.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setSmi(SearchModuleInterface smi) throws RemoteException {
		this.smi = smi;
	}

	
	@Override
	public void register_downloader(DownloaderInterface downloader) throws RemoteException {
		free_downloaders.add(downloader);
		downloaders.add(downloader);

		System.out.println("New downloader registed.");
		try{
			smi.print_status();
		}catch (RemoteException | NullPointerException e){
			// No smi
		}
	}

	@Override
	public void append_url(String url) throws RemoteException {
		if (my_queue.size() > 500) return;

		System.out.println(url + " recived");
		my_queue.add(url);
	}

	@Override
	public void extend_urls(List<String> urls) throws RemoteException {
		if (my_queue.size() > 500) return;
		
		for (String url : urls) {
			System.out.println(url + " added to queue");
			my_queue.add(url);
		}
	}

	@Override
	public void ping() throws RemoteException {
	}

	@Override
	public List<ProxyStatus> get_status() throws RemoteException{
		List<ProxyStatus> resp = new ArrayList<>();

		for (DownloaderInterface downloader : downloaders) {
			String proxyString = downloader.toString();
			
			int startIndex = proxyString.indexOf("endpoint:[") + 10;
			
			proxyString = proxyString.substring(startIndex);
			int endIndex = proxyString.indexOf("]");

			String endpoint = proxyString.substring(0, endIndex); // "endpoint:[127.0.0.1:50087](remote)"
			
			String[] parts = endpoint.split(":"); // ["endpoint", "127.0.0.1", "50087"](remote)
			String ip = parts[0];
			String port = parts[1];

			resp.add(new ProxyStatus(ip, Integer.parseInt(port)));
		}

		return resp;
	}

	@Override
	public void remove_down(DownloaderInterface d) throws RemoteException {
		downloaders.remove(d);
		
		try{
			smi.print_status();
		}
		catch (NullPointerException | RemoteException e){
			// no smi
		}
	}

	
	public static void main(String[] args) throws RemoteException {
		Integer port;

		if (args.length == 0){
			port = 1099;
		}else {
			if (args[0].equals("--help")){
				System.out.println("Correct usage\n\t queue.jar <\"port\"_to_host_on_local>");
				return;
			}
			port = Integer.parseInt(args[0]);
			System.out.println("Port defined to default (1099)");
		}

		QueueInterface qi = new Queue();
		
		LocateRegistry.createRegistry(port).rebind("queue_mod", qi);

		System.out.println("Queue Module Ready");

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			((Queue) qi).on_end();
		}));
	}
}