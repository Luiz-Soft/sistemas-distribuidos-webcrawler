import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.HashMap;

public class IndexStorageBarrel {
    private static final String MULTICAST_GROUP = "224.3.2.1";
    private static final int PORT = 4321;
    private static final int BUFFER_SIZE = 4096;

    private HashMap<String, String[]> index;

    private MulticastSocket socket;

    public IndexStorageBarrel() {
        index = new HashMap<String, String[]>();
    }

    public void run() {
        try {
            InetAddress group = InetAddress.getByName(MULTICAST_GROUP);
            socket = new MulticastSocket(PORT);
            socket.joinGroup(group);

            byte[] buffer = new byte[BUFFER_SIZE];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            while (true) {
                socket.receive(packet);

                String data = new String(packet.getData(), 0, packet.getLength());
                String[] indexData = data.split(",");

                try {
                    String word = indexData[0];
                    String url = indexData[1];
                    if (index.containsKey(word)) {
                        String[] urls = index.get(word);
                        String[] newUrls = new String[urls.length + 1];
                        System.arraycopy(urls, 0, newUrls, 0, urls.length);
                        newUrls[urls.length] = url;
                        index.put(word, newUrls);
                    } else {
                        index.put(word, new String[] { url });
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    System.out.println("Received malformed data. Ignoring...");
                }

                System.out.println("index updated");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                InetAddress group = InetAddress.getByName(MULTICAST_GROUP);
                if (socket != null && !socket.isClosed()) {
                    socket.leaveGroup(group);
                    socket.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Expose a method to send index data
    public void sendIndex(String word, String url) {
        try {
            InetAddress group = InetAddress.getByName(MULTICAST_GROUP);

            String data = word + "," + url;
            byte[] buffer = data.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, PORT);
            socket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        IndexStorageBarrel barrel = new IndexStorageBarrel();
        barrel.run();
    }
}
