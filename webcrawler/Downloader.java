package webcrawler;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class Downloader{
  
  // Define o endereço de multicast e a porta
  private static final String MULTICAST_ADDRESS = "224.0.0.1";
  private static final int PORT = 4446;
  
  // Define o número de threads do pool de threads
  private static final int THREAD_POOL_SIZE = 10;
  
  // Define o executor de threads
  private static ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
  
  public  void download(String [] urls) throws SocketException, UnknownHostException {
    // Cria um socket UDP para enviar os dados multicast
    DatagramSocket socket = new DatagramSocket();
    InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
    
    // Define as URLs que serão baixadas e analisadas
   
    
    // Inicia o download de cada URL em uma thread separada
    for (String url : urls) {
      executor.execute(() -> {
        try {
          // Faz o download da página web usando o Jsoup
          Document doc = Jsoup.connect(url).get();
          // Extrai as informações relevantes da página e atualiza o índice
          String title = doc.title();
          String content = doc.text();
          updateIndex(title, content);
          
          // Cria um pacote de dados multicast com as informações atualizadas
          byte[] data = (title + "\n" + content).getBytes();
          DatagramPacket packet = new DatagramPacket(data, data.length, group, PORT);
          
          // Envia o pacote multicast
          socket.send(packet);
        } catch (IOException e) {
          e.printStackTrace();
        }
      });
    

    }
    
    executor.shutdown();
    try {
        executor.awaitTermination(1, TimeUnit.MINUTES);
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
    socket.close();
  }
  
  private static void updateIndex(String title, String content) {
    // Implemente aqui a lógica para atualizar o índice
    // com as informações extraídas da página
    System.out.println(title);
    System.out.println(content);

  }
}
