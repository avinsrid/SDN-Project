import java.net.*;
import java.io.*;
import java.lang.Runtime;
import java.lang.Process;

public class IncomingCmd extends Thread {
	private ServerSocket serverSocket;

	public IncomingCmd(int port) throws IOException {
		serverSocket = new ServerSocket(port);
		serverSocket.setSoTimeout(0);
	}

	public void run() {
		while (true) {
			System.out.println("IncomingCmd:: Thread Started");
			try {
				System.out.println("IncomingCmd:: Listening on socket " + serverSocket.getLocalPort());
				Socket server = serverSocket.accept();
				DataInputStream in = new DataInputStream(server.getInputStream());
				String checkInput = in.readUTF();
				System.out.println(checkInput);
				if (checkInput.equals("Delete-s5")) {
					try {
						Process p = Runtime.getRuntime().exec("sudo ovs-vsctl del-port s5 s5-eth1");
						System.out.println("Executed");
					}
					catch (IOException e) { System.out.println("IncomingCmd:: Error deleting switch 5 port s5-eth1"); }
				}
				else if (checkInput.equals("Delete-s6")) {
					try {
						Process p = Runtime.getRuntime().exec("sudo ovs-vsctl del-port s6 s6-eth1");
					}
					catch (IOException e) {System.out.println("IncomingCmd:: Error deleting switch 6 port s6-eth1"); }
				}
				else if (checkInput.equals("Pull-s5")) {
					try {
						Process p = Runtime.getRuntime().exec("sudo ovs-vsctl add-port s5 s5-eth1");
					}
					catch (IOException e) {System.out.println("IncomingCmd:: Error adding switch 5 port s5-eth1"); }
				}
				else if (checkInput.equals("Pull-s6")) {
					try {
						Process p = Runtime.getRuntime().exec("sudo ovs-vsctl add-port s6 s6-eth1");
					}
					catch(IOException e) {System.out.println("IncomingCmd:: Error adding switch 6 port s6-eth1"); }
				}
			}
			catch (SocketTimeoutException s) {System.out.println("IncomingCmd:: Socket Timed Out"); }
			catch (IOException e) {
				System.out.println("IncomingCmd:: Error receiving data from socket");
				e.printStackTrace();
			}
		}
	}
	public static void main(String[] args) {
		System.out.println("IncomingCmd:: Started");
		int port1 = 41201;
		int port2 = 41202;
		try {
			Thread t1 = new IncomingCmd(port1);
			Thread t2 = new IncomingCmd(port2);
			t1.start();
			t2.start();

		}
		catch (IOException e) {
			System.out.println("IncomingCmd:: Error starting thread");
			e.printStackTrace();
		}
	}

}