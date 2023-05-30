import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class MyClient {

	static boolean debug = false;

	public static void main(String[] args) {

		try {
			//Create a socket
			//Keep track of the largest server type and the number of servers of that type
			String typeCapableServer = "";
			int idCapableServer = 0;
			//Boolean keeping track of whether nRecs should be processed or not
			boolean serversCalculated = false;
			//String to track what the last message the Server sent to the Client
			String messageTracker;
			//String array to store job data
			String[] jobData = new String[0];
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
			sendData("AUTH " + username, dataOut);

			//Receive OK
			messageTracker = receiveCompliantData("OK", dataReader);

			//While the last message from ds-server is not NONE do // jobs n - 1
			while(!messageTracker.isEmpty() && !messageTracker.equals("NONE")){
				//Send REDY
				sendData("REDY", dataOut);

				//Receive a message // typically one of the following: JOBN, JCPL, NONE
				jobData = processJob(receiveData(dataReader));

				//Identify the best-fit server type as long as the last message contained job information
				if(!serversCalculated && jobData[0].equals("JOBN")) {
					//Send a GETS Capable message
					sendData("GETS Capable " + jobData[2] + " " + jobData[3] + " " + jobData[4], dataOut);

					//Receive DATA nRecs recSize
					String[] calculatedServers = calculateServers(receiveData(dataReader), dataOut, dataReader, jobData[2]);
					typeCapableServer = calculatedServers[0];
					idCapableServer = Integer.parseInt(calculatedServers[1]);
					serversCalculated = true;
					//Receive .
					messageTracker = receiveCompliantData(".", dataReader);
				}

				//If the message received at Step 10 is JOBN
				if(jobData[0].equals("JOBN")){
					//Schedule job and increment serverToTask so that the next job is scheduled to a different server of the same type
					scheduleJob(jobData, typeCapableServer, idCapableServer, dataOut);
					serversCalculated = false;
					messageTracker = receiveCompliantData("OK", dataReader);
				} else if(jobData[0].equals("JCPL")) {
					serversCalculated = true;
				} else {
					if(debug) {
						System.out.println("Received " + jobData[0] + ". Failed to schedule, expected JOBN.");
					}
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
		if(debug) {
			System.out.println("Sent " + s + ".");
		}
		o.flush();
	}

	public static String receiveData(BufferedReader b) throws IOException {
		String messageReceived = b.readLine();
		if(debug) {
			System.out.println("Received " + messageReceived + ".");
		}
		return messageReceived;
	}

	public static String receiveCompliantData(String s, BufferedReader b) throws IOException {
		if(s != null && !s.isEmpty()) {
			String messageReceived = b.readLine();
			if (messageReceived.equals(s)) {
				if(debug) {
					System.out.println("Received " + messageReceived + ". Authenticated.");
				}
			} else {
				if(debug) {
					System.out.println("Received " + messageReceived + ". Failed to authenticate, expected " + s + ".");
				}
			}
			return messageReceived;
		} else {
			if(debug) {
				System.out.println("Received a null or empty String. Failed to authenticate.");
			}
			return "NONE";
		}
	}

	public static String[] calculateServers(String s, OutputStream o, BufferedReader b, String ncpu) throws IOException {
		//Send OK
		sendData("OK", o);

		//Receive each record
		String[] serverData = s.split(" ");
		int numServers = Integer.parseInt(serverData[1]);
		ArrayList<String[]> serverDataArray = new ArrayList<String[]>();

		if(debug) {
			System.out.println("Created empty serverDataArray Array List with length " + numServers + ".");
		}

		for(int i = 0; i < numServers; i++){
			s = b.readLine();
			if(debug) {
				//System.out.println(i + " is still less than " + numServers + " continue reading and splitting.");
				System.out.println("Server at location " + i + " has info: " + s + ".");
			}
			serverDataArray.add(s.split(" "));
			if(debug) {
				System.out.println("Splitting server " + i + " info into serverDataArray location " + i + ".");
			}
		}

		if(debug) {
			System.out.println("Successfully split all server info into serverDataArray" + ".");
		}

		String typeTargetServer = null;
		String idTargetServer = null;
		int numCPU = Integer.parseInt(ncpu);
		boolean foundTargetServer = false;

		if(debug) {
			System.out.println("Required number of CPUs is " + numCPU + ".");
		}

		//get the named type of the best fitting server and its ID
		for(int i = 0; i < serverDataArray.size(); i++){
			if(debug) {
				System.out.println("Finding best-fit server type and ID. Server " + i + " has CPU count of " + Integer.parseInt(serverDataArray.get(i)[4]) + " which is compared against required CPU count of " + numCPU + ".");
			}
			if(numCPU <= Integer.parseInt(serverDataArray.get(i)[4]) && foundTargetServer == false) { // finds best-fit server
				typeTargetServer = serverDataArray.get(i)[0];
				idTargetServer = serverDataArray.get(i)[1];
				if(debug) {
					System.out.println("Best-fit server identified. Type is " + typeTargetServer + " and ID is " + idTargetServer + ", and has a CPU count of " + Integer.parseInt(serverDataArray.get(i)[4]) + ".");
				}
				foundTargetServer = true;
			}
		}

		String[] calculatedServers = new String[2];

		if(!foundTargetServer){
			if(debug){
				System.out.println("No best-fit server identified. Returning the first sorted server in the array list. First server ID is " + serverDataArray.get(serverDataArray.size()-1)[1] + " and is of type " + serverDataArray.get(serverDataArray.size()-1)[0] + ".");
			}

			calculatedServers[0] = serverDataArray.get(0)[0];
			calculatedServers[1] = serverDataArray.get(0)[1];

			//Send OK
			sendData("OK", o);

			return calculatedServers;
		} else {
			if(debug) {
				System.out.println("Best-fit server identified. Best-fit server ID is " + idTargetServer + " and is of type " + typeTargetServer + ".");
			}

			calculatedServers[0] = typeTargetServer;
			calculatedServers[1] = idTargetServer;

			//Send OK
			sendData("OK", o);

			return calculatedServers;
		}
	}

	public static String[] processJob(String s) {
		//improved dataset management
		if(s != null && !s.isEmpty()) {
			String[] jobData = s.split(" ");
			String[] returnData = new String[5];
			if (jobData[0].equals("JOBN")) {
				returnData[0] = jobData[0]; // message type
				returnData[1] = jobData[2]; // job ID
				returnData[2] = jobData[4]; // required cores
				returnData[3] = jobData[5]; // required memory
				returnData[4] = jobData[6]; // required disk space
			} else {
				returnData[0] = jobData[0]; // message type failsafe
				if(debug) {
					System.out.println("Received " + returnData[0] + ". Failed to process job, expected JOBN.");
				}
			}
			return returnData;
		} else {
			if(debug) {
				System.out.println("Received a null or empty String. Failed to process job.");
			}
			return null;
		}
	}

	public static void scheduleJob(String[] sa, String s, int i, OutputStream o) throws IOException {
		//Schedule a job // SCHD
		if(debug) {
			System.out.println(sa[0] + " received, start scheduling job with job ID as " + sa[1] + ", to " + s + "-type server with server ID of " + i + ".");
		}
		String jobMessage = "SCHD " + sa[1] + " " + s + " " + i;
		sendData(jobMessage, o);
	}
}
