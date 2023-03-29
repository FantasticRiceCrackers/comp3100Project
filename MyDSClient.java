import java.io.*;
import java.net.*;
import java.util.*;

public class MyDSClient {

	public static void main(String[] args) {

		try {
			//initialise socket, input-output streams and reader
			Socket sckt = new Socket("localhost",50000);
			OutputStream dataOut = sckt.getOutputStream();
			InputStream dataIn = sckt.getInputStream();
			BufferedReader dataReader = new BufferedReader(new InputStreamReader(dataIn));
			
			//for(i=0;i<7;i++){
				//BufferedReader dataReader = new BufferedReader(new InputStreamReader(dataIn));
			//}
			
			//send HELO
			String messageToSend = "HELO\n";
			dataOut.write(messageToSend.getBytes());
			String messageReceived = dataReader.readLine();
			System.out.println(messageReceived);
			dataOut.flush();
			
			//get username and send AUTH
			String username = System.getProperty("user.name");
			messageToSend = "AUTH" + username + "\n";
			dataOut.write(messageToSend.getBytes());
			messageReceived = dataReader.readLine();
			System.out.println(messageReceived);
			dataOut.flush();
			
			//send REDY
			messageToSend = "REDY\n";
			dataOut.write(messageToSend.getBytes());
			messageReceived = dataReader.readLine();
			System.out.println(messageReceived);
			dataOut.flush();
			
			//send GETS
			messageToSend = "GETS All\n";
			dataOut.write(messageToSend.getBytes());
			messageReceived = dataReader.readLine();
			System.out.println(messageReceived);
			dataOut.flush();
			
			//send OK
			messageToSend = "OK\n";
			dataOut.write(messageToSend.getBytes());
			dataOut.flush();
			
			//process data
			String[] data = messageReceived.split(" ");
			int numServers = Integer.parseInt(data[1]);
			System.out.println(numServers);
			
			ArrayList<String[]> serverData = new ArrayList<String[]>();
			
			for(int i = 0; i < numServers; i++){
				messageReceived = dataReader.readLine();
				System.out.println(messageReceived);
				//String[] serverData = messageReceived.split(" ");
				serverData.add(messageReceived.split(" "));
			}
			
			String largestServer = " ";
			
			for(int i = 0; i < serverData.size(); i++){
				int numCPU = 0;
				if(numCPU < Integer.parseInt(serverData.get(i)[4])) {
					//numCPU = Integer.parseInt(serverData[4]);
					largestServer = serverData.get(i)[0];
				}
			}
			
			//send OK
			messageToSend = "OK\n";
			dataOut.write(messageToSend.getBytes());
			dataOut.flush();
			
			//schedule and send jobs
			//int numCPU = Integer.parseInt(job[?]);
			messageToSend = "SCHD " + "0 " + largestServer + " 0\n";
			
			dataOut.write(messageToSend.getBytes());
			messageReceived = dataReader.readLine();
			System.out.println(messageReceived);
			dataOut.flush();
			
			//send QUIT after confirm job scheduled
			dataOut.write(("QUIT\n").getBytes());
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
