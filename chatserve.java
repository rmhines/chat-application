/******************************************************************
 * Project: Client-Server Network Application (Chat Program)
 *
 * Programmer: Bobby Hines
 *
 * Description:
 * This is the "server" side of the chat program, although it has 
 * a lot in common with the "client" program. Notable differences
 * are that this program must be initiated first, and awaits 
 * a connection from a host running the client app. It also makes 
 * use of an explicit background process for receiving messages 
 * that runs concurrently with the main thread that processes 
 * messages sent from the server side. This app will also run 
 * indefinitely, waiting for client connections, until a SIGINT 
 * (ctrl+c or cmd+c) tells it to die. 
 *
 * Input: The user must specify a port number in order to start
 * this program. This is passed as a command line argument. It also 
 * takes in any messages typed from this end and sends them to the 
 * client socket. 
 *
 * Output: The only output are messages received from client peers
 * (displayed through this program) and messages sent to client 
 * peers (displayed through their console). 
 *
 ******************************************************************/

import java.net.*; // Sockets
import java.io.*; // Basic input/output

public class chatserve {

    // This flag controls the background thread listener
    public volatile boolean keepRunning;
    // This flag determines whether or not to continue waiting for client connections
    public volatile boolean restart;

    public static void main(String[] args) throws IOException {
        // Instantiate the class to be able to call its methods
        chatserve chatServer = new chatserve();
        // This program runs until a SIGINT (ctrl+c) stops it
        while(true) {
            // Start the server, passing in any command line arguments
            chatServer.startServer(args);
        }
    }

    private void startServer(String [] args) {
        // Initially set the restart flag to false so as to not restart immediately
        restart = false;
        // Initialize the portnum to something invalid
        int portNumber = 0;
        // The server's handle is hardcoded to SERVER. Set to final for thread access
        final String serverHandle = "SERVER";

        // Check that a port argument was passed in
        if (args.length != 1) {
            System.err.println("Usage: java chatserve <port number>");
            System.exit(1);
        } 

        // Check that port can be parsed to an integer
        try {
            portNumber = Integer.parseInt(args[0]);
        } catch(NumberFormatException e){
            System.err.println("Error: Port must be a number between 1024 and 65535.");
            System.exit(1);
        }

        // Check that port is between 1024 and 65535 (0-1023 may be in use or reserved)
        if (portNumber < 1024 || portNumber > 65535) {
            System.err.println("Error: Port must be a number between 1024 and 65535.");
            System.exit(1);
        }

        // Create buffer for storing user input from this host
        BufferedReader stdIn =
            new BufferedReader(
                new InputStreamReader(System.in));

        try {
            // Create a server socket that listens on the specified port
            final ServerSocket serverSocket =
                new ServerSocket(portNumber);

            System.out.println("Awaiting incoming connections on port " + portNumber);

            // Accept connections from clients
            final Socket clientSocket = serverSocket.accept(); 

            // Notify user that a connection has been established with an outside client
            if (clientSocket.isConnected()) {
                System.out.println("Established connection with new client at " + 
                    clientSocket.getRemoteSocketAddress()); 
            }

            // Create output stream for sending messages to client
            PrintWriter out =
                new PrintWriter(clientSocket.getOutputStream(), true);   

            // Create buffer to store message text from client
            final BufferedReader in = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream()));

            // Define background task for accepting messages from peer
            Runnable r = new Runnable() {
                public void run() {
                    // Print message from peer (their handle is appended on their end)
                    try {
                        String str = "";
                        // Only run this process while the volatile flag is set to true
                        while(keepRunning) {
                            str = in.readLine();
                            // If the received string is null, there was a connection problem
                            if (str != null) {
                                System.out.print("(incoming message)\n" + 
                                    str + "\n" + serverHandle + "> ");
                            } else {
                                // Connection lost with client, stop waiting for input
                                break;
                            }
                        }

                        // Kill any background process
                        keepRunning = false;
                        // We want to restart to await future connections
                        restart = true;
                        // Close any sockets currently opened
                        serverSocket.close();
                        clientSocket.close();

                        System.out.println("\nClient has disconnected.");
                        System.out.println("Press enter to begin listening for new client.");

                        return;
                    } catch (IOException e) {
                        // If an exception is caught, kill this thread and exit the program
                        keepRunning = false;
                        System.err.println("Error: An IOException was thrown.");
                        System.exit(1);
                    } 
                }
            };

            // If the background daemon has called for a server restart, return to main
            if (restart) {
                // Close any open sockets to avoid interference with future sockets
                serverSocket.close();
                clientSocket.close();
                return;
            }

            // Create the background thread, passing in the Runnable listener
            Thread bg = new Thread(r);
            // Set the thread to be a daemon, so that it dies along with parent processes
            bg.setDaemon(true);
            // Tell the thread that it's okay to start listening now
            keepRunning = true;
            // Finally, spin up the process to run concurrently in the background
            bg.start();

            // Initialize the user input string to an empty, yet valid string
            String inputLine = "";

            // Wait for input and send unless null or "\quit", or flag set to false
            while (inputLine != null && !(inputLine.equals("\\quit"))
                && keepRunning) {
                // Display the prompt, e.g. "SERVER> "
                System.out.print(serverHandle + "> ");

                // Get the user input
                inputLine = stdIn.readLine();

                // Verify that the input was received okay and wasn't a quit command
                if (inputLine != null || !(inputLine.equals("\\quit"))) {
                    // Send the message along with the SERVER handle prompt
                    out.println(serverHandle + "> " + inputLine);
                } else {
                    // Kill the background thread and exit
                    keepRunning = false;
                    System.exit(0);
                }
            }
        } catch (IOException e) {
            // Catch IO exceptions and display the error message
            System.err.println("Exception caught when trying to listen on port "
                + portNumber + " or listening for a connection");
            System.err.println(e.getMessage());
        }
    }
}
