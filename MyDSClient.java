import java.io.*;
import java.net.*;

public class MyDSClient {

	public static void main(String[] args) {

		try {
			//initialise socket, input-output streams and reader
			Socket sckt = new Socket("localhost",50000);
			OutputStream dataOut = sckt.getOutputStream();
			InputStream dataIn = sckt.getInputStream();
			BufferedReader dataReader = new BufferedReader(new InputStreamReader(dataIn));
			
			//send HELO
			String messageToSend = "HELO\n";
			dataOut.write(messageToSend.getBytes());
			String messageReceived = dataReader.readLine();
			System.out.println(messageReceived);
			
			//send AUTH
			String username = System.getProperty("user.name");
			messageToSend = "AUTH" + username + "\n";
			dataOut.write(messageToSend.getBytes());
			messageReceived = dataReader.readLine();
			System.out.println(messageReceived);
			
			//send REDY
			messageToSend = "REDY\n";
			dataOut.write(messageToSend.getBytes());
			messageReceived = dataReader.readLine();
			System.out.println(messageReceived);
			
			//send QUIT after confirming REDY
			dataOut.write("QUIT\n".getBytes());
			dataOut.flush();
			sckt.close();
			dataIn.close();
			dataOut.close();
			
			//Socket sckt = new Socket("localhost",6666);
			//DataOutputStream dataOut = new DataOutputStream(sckt.getOutputStream());
			//dataOut.writeUTF("HELO");
			//dataOut.flush();
			
			//DataInputStream dis = new DataInputStream(sckt.getInputStream());
			//String str = (String)dis.readUTF();
			//System.out.println("Client received = " + str);
			//dataOut.writeUTF("BYE");
			//dataOut.flush();
			
			//str = (String)dis.readUTF();
			//System.out.println("Client received = " + str);
			
			//dataOut.close();
			//sckt.close();

		} catch(Exception e) {
			System.out.println(e);

		}
	}
}
