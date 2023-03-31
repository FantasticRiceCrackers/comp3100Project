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
			System.out.println("Sent HELO and received " + messageReceived);
			dataOut.flush();
			
			//get username and send AUTH
			String username = System.getProperty("user.name");
			messageToSend = "AUTH" + username + "\n";
			dataOut.write(messageToSend.getBytes());
			messageReceived = dataReader.readLine();
			System.out.println("Confirmed username as " + username + " and received " + messageReceived);
			dataOut.flush();
			
			//send REDY
			messageToSend = "REDY\n";
			dataOut.write(messageToSend.getBytes());
			messageReceived = dataReader.readLine();
			System.out.println("Sent REDY and received " + messageReceived);
			String[] jobData = messageReceived.split(" ");
			String jobName = " ";
			//print out job data for better readability of jobs
			for(int i = 0; i < jobData.length; i++){
				System.out.println("Job data at array location " + i + " is " + jobData[i]);
			}
			if(jobData[0].equals("JOBN") || jobData[0].equals("JCPL") || jobData[0].equals("NONE")){
				jobName = jobData[0];
				System.out.println("Job name set to " + jobName);
			}
			dataOut.flush();
			
			//while the last message from ds-server is not NONE do // jobs 1 - n
			//if(!jobName.equals("NONE")){
			//	int jobNum = jobData.length - 1;
			//	while(jobNum != 0){
			//		
			//	}
			//}
			
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
			String[] serverData = messageReceived.split(" ");
			int numServers = Integer.parseInt(serverData[1]);
			System.out.println(numServers);
			
			ArrayList<String[]> serverDataArray = new ArrayList<String[]>();
			
			for(int i = 0; i < numServers; i++){
				messageReceived = dataReader.readLine();
				System.out.println(messageReceived);
				//String[] serverDataArray = messageReceived.split(" ");
				serverDataArray.add(messageReceived.split(" "));
			}
			
			String largestServer = " ";
			
			for(int i = 0; i < serverDataArray.size(); i++){
				int numCPU = 0;
				if(numCPU < Integer.parseInt(serverDataArray.get(i)[4])) {
					//numCPU = Integer.parseInt(serverDataArray[4]);
					largestServer = serverDataArray.get(i)[0];
				}
			}
			
			//send OK
			messageToSend = "OK\n";
			dataOut.write(messageToSend.getBytes());
			dataOut.flush();
			
			//schedule and send jobs
			//while the last message from ds-server is not NONE do // jobs 1 - n
			if(jobName.equals("JOBN")){
				
				System.out.println("JOBN received, start scheduling job with job ID as " + jobData[2]);
				messageToSend = "SCHD " + jobData[2] + " " + largestServer + " 0\n";
				
				//int jobNum = jobData.length - 1;
				//int jobID = 1;
				//System.out.println("Job name is set to allowed ID, start scheduling jobs with first job as " + jobData[jobID] + " and amount of jobs being " + jobNum);
				//while(jobNum != 0){
				//	messageToSend = "SCHD " + jobData[jobID] + " " + largestServer + " 0\n";
				//	jobID++;
				//	jobNum--;
				//}
			}
			dataOut.write(messageToSend.getBytes());
			messageReceived = dataReader.readLine();
			System.out.println(messageReceived);
			dataOut.flush();
			
			//send JCPL
			messageToSend = "JCPL\n";
			dataOut.write(messageToSend.getBytes());
			messageReceived = dataReader.readLine();
			System.out.println("Sent JCPL and received " + messageReceived);
			dataOut.flush();
			
			//send QUIT after confirm job scheduled
			dataOut.write(("QUIT\n").getBytes());
			dataOut.flush();
			sckt.close();
			dataIn.close();
			dataOut.close();

		} catch(Exception e) {
			System.out.println(e);

		}
	}
}
