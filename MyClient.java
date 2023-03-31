import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class MyClient {

	public static void main(String[] args) {

		try {
			//Create a socket
			//Keep track of the largest server type and the number of servers of that type
			String typeLargestServer = "";
			int numLargestServer = 0;
			//Int to track which exact server to schedule a job to
			int serverToTask = 0;
			//Boolean keeping track of whether nRecs has been processed or not
			boolean serversCalculated = false;
			//String to track what the last message the Server sent to the Client
			String messageTracker;
			//String array to store job data
			String[] jobData;
			//The socket itself, begins not initialised
			Socket sckt;

			//Initialise input and output streams with the socket
			OutputStream dataOut;
			InputStream dataIn;
			BufferedReader dataReader;

			//Connect ds-server
			sckt = new Socket("localhost",50000);
			dataOut = sckt.getOutputStream();
			dataIn = sckt.getInputStream();
			dataReader = new BufferedReader(new InputStreamReader(dataIn));

			//Send HELO
			sendData("HELO", dataOut);

			//Receive OK
			messageTracker = receiveCompliantData("OK", dataReader);

			//Send AUTH username
			String username = System.getProperty("user.name");
			sendData("AUTH" + username, dataOut);

			//Receive OK
			messageTracker = receiveCompliantData("OK", dataReader);

			//While the last message from ds-server is not NONE do // jobs n - 1
			while(messageTracker != null && !messageTracker.isEmpty() && !messageTracker.equals("NONE")){
				//Send REDY
				sendData("REDY", dataOut);

				//Receive a message // typically one of the following: JOBN, JCPL, NONE
				jobData = processJob(receiveData(dataReader));
				messageTracker = jobData[0];

				//Identify the largest server type; you may only do this once
				if(!serversCalculated) {
					//Send a GETS All message
					sendData("GETS All", dataOut);

					//Receive DATA nRecs recSize
					String[] calculatedServers = calculateServers(receiveData(dataReader), dataOut, dataReader);
					typeLargestServer = calculatedServers[0];
					numLargestServer = Integer.parseInt(calculatedServers[1]);
					serverToTask = 0;
					serversCalculated = true;
					//Receive .
					messageTracker = receiveCompliantData(".", dataReader);
				} else {

				}

				//If the message received at Step 10 is JOBN
				if(jobData[0] != null && !jobData[0].isEmpty() && jobData[0].equals("JOBN")){
					//Make sure the jobs are being distributed evenly among the largest servers
					if(serverToTask == numLargestServer) {
						serverToTask = 0;
					}
					//Schedule job and increment serverToTask so that the next job is scheduled to a different server of the same type
					scheduleJob(jobData, typeLargestServer, serverToTask++, dataOut, dataReader);
					messageTracker = receiveCompliantData("OK", dataReader);
				} else if(jobData[0] != null && !jobData[0].isEmpty() && jobData[0].equals("OK")){
					continue;
				} else if(jobData[0] != null && !jobData[0].isEmpty() && jobData[0].equals("JCPL")) {
					continue;
				} else {
					System.out.println("Received " + jobData[0] + ". Failed to schedule, expected JOBN.");
					break;
				}
			}

			//Send QUIT
			sendData("QUIT", dataOut);

			//Receive QUIT
			receiveCompliantData("QUIT", dataReader);

			//Close the socket
			dataIn.close();
			dataOut.close();
			sckt.close();

		} catch(Exception e) {
			System.out.println(e);
		}


	}

	public static void sendData(String s, OutputStream o) throws IOException {
		String messageToSend = s + "\n";
		o.write(messageToSend.getBytes());
		System.out.println("Sent " + s + ".");
		o.flush();
	}

	public static String receiveData(BufferedReader b) throws IOException {
		String messageReceived = b.readLine();
		System.out.println("Received " + messageReceived + ".");
		return messageReceived;
	}

	public static String receiveCompliantData(String s, BufferedReader b) throws IOException {
		if(s != null && !s.isEmpty()) {
			String messageReceived = b.readLine();
			if (messageReceived.equals(s)) {
				System.out.println("Received " + messageReceived + ". Authenticated.");
			} else {
				System.out.println("Received " + messageReceived + ". Failed to authenticate, expected " + s + ".");
			}
			return messageReceived;
		} else {
			System.out.println("Received a null or empty String. Failed to authenticate.");
			return "NONE";
		}
	}

	public static String[] calculateServers(String s, OutputStream o, BufferedReader b) throws IOException {
		//Send OK
		sendData("OK", o);

		//Receive each record
		String[] serverData = s.split(" ");
		int numServers = Integer.parseInt(serverData[1]);
		ArrayList<String[]> serverDataArray = new ArrayList<String[]>();

		System.out.println("Created empty serverDataArray Array List with length " + numServers + ".");

		for(int i = 0; i < numServers; i++){
			s = b.readLine();
			//System.out.println(i + " is still less than " + numServers + " continue reading and splitting.");
			System.out.println("Server at location " + i + " has info: " + s + ".");
			serverDataArray.add(s.split(" "));
			System.out.println("Splitting server " + i + " info into serverDataArray location " + i + ".");
		}

		System.out.println("Successfully split all server info into serverDataArray" + ".");

		String largestServer = " ";
		int numLargestServers = 0;
		int numCPU = 0;

		//get the named type of the largest server
		for(int i = 0; i < serverDataArray.size(); i++){
			if(numCPU < Integer.parseInt(serverDataArray.get(i)[4])) {
				largestServer = serverDataArray.get(i)[0];
				numCPU = Integer.parseInt(serverDataArray.get(i)[4]);
			}
		}

		//get the number of servers of the largest type
		for(int i = 0; i < serverDataArray.size(); i++){
			if(numCPU == Integer.parseInt(serverDataArray.get(i)[4])) {
				numLargestServers++;
			}
		}

		System.out.println("Finished calculating serverDataArray. There are " + numLargestServers + " servers of largest type " + largestServer + ".");
		String[] calculatedServers = new String[2];
		calculatedServers[0] = largestServer;
		calculatedServers[1] = String.valueOf(numLargestServers);

		//Send OK
		sendData("OK", o);

		return calculatedServers;
	}

	public static String[] processJob(String s) {
		//improved dataset management
		if(s != null && !s.isEmpty()) {
			String[] jobData = s.split(" ");
			String[] returnData = new String[4];
			if (jobData[0].equals("JOBN")) {
				returnData[0] = jobData[0]; // message type
				returnData[1] = jobData[2]; // job ID
				returnData[2] = jobData[4]; // estimated runtime units
				returnData[3] = jobData[5]; // required processors
			} else {
				returnData[0] = jobData[0]; // message type failsafe
				System.out.println("Received " + returnData[0] + ". Failed to authenticate, expected JOBN.");
			}
			return returnData;
		} else {
			System.out.println("Received a null or empty String. Failed to authenticate.");
			return null;
		}

		//print out job data for better readability of jobs
		//System.out.println("Number of job data lines is " + jobData.length + ".");

		//for(int i = 0; i < jobData.length; i++){
		//	System.out.println("Job data at array location " + i + " is " + jobData[i]);
		//}
	}

	public static void scheduleJob(String[] sa, String s, int i, OutputStream o, BufferedReader b) throws IOException {
		//Schedule a job // SCHD
		System.out.println(sa[0] + " received, start scheduling job with job ID as " + sa[1] + ", with number of " + s + "-type servers to schedule to being " + i + ".");
		String jobMessage = "SCHD " + sa[1] + " " + s + " " + i + "\n";
		sendData(jobMessage, o);
	}
}
