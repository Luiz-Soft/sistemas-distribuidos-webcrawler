package queue;


import downloader.DownloaderInterface;
import search_module.SearchModuleInterface;
import utils.ProxyStatus;

import java.io.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;

class Queue extends UnicastRemoteObject implements QueueInterface{
    private ConcurrentLinkedDeque<DownloaderInterface> free_downloaders;
    private ConcurrentLinkedDeque<DownloaderInterface> downloaders;
    private ConcurrentLinkedQueue<String> my_queue;
    private ConcurrentHashMap<String, Boolean> already_seen;
    private SearchModuleInterface smi;

    protected Queue() throws RemoteException {
        super();
        load_from_file();

        this.free_downloaders = new ConcurrentLinkedDeque<>();
        this.downloaders = new ConcurrentLinkedDeque<>();
        tryAssignDownloaders();
    }

    private void load_from_file(){

        my_queue = new ConcurrentLinkedQueue<>();
        already_seen = new ConcurrentHashMap<>();

        try {
            FileInputStream file = new FileInputStream("my_queue.ser");
            ObjectInputStream in = new ObjectInputStream(file);
            Queue temp = (Queue) in.readObject();

            my_queue = temp.my_queue;
            already_seen = temp.already_seen;

            file.close();
            System.out.println("Loaded by file.");

        } catch (IOException | ClassNotFoundException e) {
            System.out.println("New queue.");
        }
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

    public void on_end() {
        try {
            FileOutputStream file = new FileOutputStream("my_queue.ser");
            ObjectOutputStream out = new ObjectOutputStream(file);

            free_downloaders.clear();
            downloaders.clear();
            smi = null;

            out.writeObject(this);
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
        if (already_seen.containsKey(url)) return;

        already_seen.put(url, true);
        System.out.println(url + " recieved");
        my_queue.add(url);
    }

    @Override
    public void extend_urls(List<String> urls) throws RemoteException {
        if (my_queue.size() > 500) return;

        for (String url : urls) {
            if (already_seen.containsKey(url)) continue;
            System.out.println(url + " added to queue");
            my_queue.add(url);
            already_seen.put(url, true);
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

        if (args.length == 0 || args[0].equals("--help")){
            System.out.println("Correct usage\n\t queue.jar <\"port\"_to_host_on_local>");
            return;
        }else {
            port = Integer.parseInt(args[0]);
        }

        QueueInterface qi = new Queue();

        LocateRegistry.createRegistry(port).rebind("queue_mod", qi);

        System.out.println("Queue Module Ready");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ((Queue) qi).on_end();
        }));
    }
}