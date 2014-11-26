import java.net.*;
import java.io.*;

public class connector {
	public static void main(String[] args) {
		try {
			Socket client = new Socket("192.168.56.102", 41201);
			OutputStream outToServer = client.getOutputStream();
			DataOutputStream out = new DataOutputStream(outToServer);
			out.writeUTF("Delete-s5");
			System.out.println("Completed!");
		}
		catch (IOException e) {e.printStackTrace();}

	}
}