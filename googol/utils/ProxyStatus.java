package utils;


public class ProxyStatus extends Status {
	private String ip;
	private int port;

	public ProxyStatus(String ip, int port) {
		this.ip = ip;
		this.port = port;
	}

	@Override
	public String toString() {
		return "ip: " + ip + ":" + port;
	}
}
