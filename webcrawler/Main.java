package webcrawler;

import java.net.SocketException;
import java.net.UnknownHostException;

public class Main {
    public static void main(String[] args) throws SocketException, UnknownHostException {
        // código a ser executado quando o programa é iniciado
        String[] urls = {
            "http://www.example.com",
            "http://www.example.net",
            "http://www.example.org"
          };
        Downloader Downloader = new Downloader();
       
        Downloader.download(urls);
    }
}
