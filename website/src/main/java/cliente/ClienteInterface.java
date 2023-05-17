package cliente;

import utils.ProxyStatus;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface ClienteInterface extends Remote {
    void print_status(List<String> top10, List<List<ProxyStatus>> info) throws RemoteException;
}
