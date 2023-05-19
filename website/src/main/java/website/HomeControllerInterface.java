package website;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import utils.ProxyStatus;

public interface HomeControllerInterface extends Remote {
	public void sendMessage(List<String> top10, List<List<ProxyStatus>> info) throws RemoteException;
}
