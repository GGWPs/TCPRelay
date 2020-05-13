import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Ivan Miladinovic
 */
public class ServerMediatorRelay {

    //Ports for exchanging information/relay and punching.
    private int tcpDiscussionPort = 9000;

    private ServerSocket socketConnect;


    //List to keep track who connected
    private volatile List<String> aliasList = new ArrayList<>();
    private volatile List<BufferedReader> inList = new ArrayList<>();
    private volatile List<BufferedOutputStream> outList = new ArrayList<>();
    private volatile List<Boolean> connectedList = new ArrayList<>();
    private volatile List<Boolean> relayList = new ArrayList<>();
    private volatile List<String> relayConnectedList = new ArrayList<>();

    private Thread listen;
    private boolean exit;

    //Constructor using default tcp discussion/punch ports
    public ServerMediatorRelay() {
        try {
            runServer();
        } catch (IOException ex) {
            Logger.getLogger(ServerMediatorRelay.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    //Constructor specify tcp discussion/punch ports
    public ServerMediatorRelay(int userTcpPort) {
        this.tcpDiscussionPort = userTcpPort;
        try {
            runServer();
        } catch (IOException ex) {
            Logger.getLogger(ServerMediatorRelay.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    //Main method
    public static void main(String[] args) throws IOException {
        if (args.length > 0) {//Give args
            new ServerMediatorRelay(Integer.parseInt(args[0].trim()));
        } else {//Give no args
            new ServerMediatorRelay();
        }
    }

    //Run server listening clients
    void runServer() throws IOException {
        //Create Server Socket for accepting Client TCP connections
        System.out.println("Server started with ports, TCP connection: " + tcpDiscussionPort);
        runDiscussionServer();
        serverCommands();
    }

    //Thread to connect the clients and get information from them
    private void runDiscussionServer(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    socketConnect = new ServerSocket(tcpDiscussionPort);
                    while(!exit){ //While loops which adds each clients information to a list.
                        System.out.println("Waiting for Client");
                        //Accept first client connection
                        Socket clientAConnect = socketConnect.accept();
                        System.out.println("Client connected " + clientAConnect.getInetAddress() + " " + clientAConnect.getPort());

                        //Create input and output streams to read/write messages for CLIENT
                        BufferedReader inConnectA = new BufferedReader(new InputStreamReader(clientAConnect.getInputStream()));
                        BufferedOutputStream outConnectA = new BufferedOutputStream(clientAConnect.getOutputStream());
                        String aliasA = inConnectA.readLine(); //get alias clients sends
                        outConnectA.write("connected".getBytes()); //write connected to client to start write function on client
                        outConnectA.write('\n');
                        outConnectA.flush();
                        aliasList.add(aliasA);
                        inList.add(inConnectA);
                        outList.add(outConnectA);
                        connectedList.add(true);
                        relayList.add(false);
                        relayConnectedList.add("");
                        connectedPeer(aliasA, inConnectA, outConnectA);
                    }
                }catch (IOException ioe){
                    System.out.println("Closing down server!");
//                    ioe.printStackTrace();
                }
            }
        }).start();
    }

    //Make thread that listens to messages from peer.
    private void connectedPeer(String alias, BufferedReader client,  BufferedOutputStream output) throws IOException {
        new Thread(() -> {
            int index = aliasList.indexOf(alias);
            while (connectedList.get(index)) { //Condition to check if thread has to keep running
                try {
                        String message = client.readLine(); // Message from client
                        if(relayList.get(index) && message != null){ //if already in a relay with another client, send message to that client
                            BufferedOutputStream connectedPeer = outList.get(aliasList.indexOf(relayConnectedList.get(index)));
                            String msg2 = alias + ":" + message;
                            connectedPeer.write(msg2.getBytes());
                            connectedPeer.write('\n');
                            connectedPeer.flush();
                        } else if(message != null) { //check if not null
                            System.out.println(message);
                            String[] message2 = message.split(" ");
                            if("/alias".equals(message2[0])) { //If input from user is /alias, output to user list of aliasses
                                System.out.println(Arrays.toString(aliasList.toArray()));
                                output.write(Arrays.toString(aliasList.toArray()).getBytes());
                                output.write('\n');
                                output.flush();
                            }
                            if("/connect".equals(message2[0])) {//Check if first word is equal to /connect
                                if(message2.length > 1){ //Check length incase no alias is specified
                                    if(alias.equals(message2[1])){ //Check for self alias else it will connect to self
                                        output.write("Cant connect to self!".getBytes());
                                        output.write('\n');
                                        output.flush();
                                    } else if(aliasList.contains(message2[1])){ //Check if list contains alias.
                                        int targetClientIndex = aliasList.indexOf(message2[1]);
                                        if(!relayList.get(targetClientIndex)){
                                            connectedList.set(index, false);
                                            connectedList.set(targetClientIndex, false);
                                            relayList.set(index, true);
                                            relayList.set(targetClientIndex, true);
                                            relayConnectedList.set(index, message2[1]);
                                            relayConnectedList.set(targetClientIndex, alias);
                                            proceedInfosExchange(alias, client, output, message2[1], inList.get(targetClientIndex), outList.get(targetClientIndex));
                                        } else {//if target client already connected, let client know
                                            output.write("User already connected!".getBytes());
                                            output.write('\n');
                                            output.flush();
                                        }
                                    } else { //Else for if alias is not found
                                        output.write("Alias not found!".getBytes());
                                        output.write('\n');
                                        output.flush();
                                    }
                                } else { //If no word found after /connect, send to client error.
                                    output.write("No alias specified!".getBytes());
                                    output.write('\n');
                                    output.flush();
                                }
                            }
                        }
                } catch (IOException e ) {
                    System.out.println("Lost connection with " + alias);
                    removeClient(index);
                }
            }
        }).start();
    }


    //Start relay between two clients
    private void proceedInfosExchange(String aliasA, BufferedReader inA, BufferedOutputStream outA, String aliasB, BufferedReader inB, BufferedOutputStream outB) throws IOException{
        System.out.println("***** Starting Relay between client " + aliasA + " and client " + aliasB);
        //Send client info once and start up threads for relay.
        relayPeers(aliasA, inA, outB, aliasB); //Call function using alias from A, input from A and output to B.
        relayPeers(aliasB, inB, outA, aliasA);
    }

    //Make thread for Peer relay, function to send message from A to B!
    private void relayPeers(String alias, BufferedReader clientA, BufferedOutputStream clientB, String aliasB) throws IOException {
        new Thread(() -> {
            int index = aliasList.indexOf(alias);
            int index2 = aliasList.indexOf(aliasB);
            String userConnected = alias + " has connected with you!";
            boolean sent = false;
            while (relayList.get(index)) {
                try {
                    if(!sent){
                        clientB.write(userConnected.getBytes());     //Send message to peer
                        clientB.write('\n');
                        clientB.flush(); //empty buffer
                        sent = true;
                    }
                    String message = clientA.readLine(); // Message from clientA
                    if(message != null) {
                        if("/disconnect".equals(message)) { //If input from user is /disconnect, disconnect from chat
                            String msg2 = alias + ": has disconnected!";
                            String discMsg = "Disconnected from chat with " + aliasB;
                            BufferedOutputStream clientOut = outList.get(index);
                            clientOut.write(discMsg.getBytes());
                            clientOut.write('\n');
                            clientOut.flush();
                            clientB.write(msg2.getBytes());     //Send message to peer
                            clientB.write('\n');
                            clientB.flush(); //empty buffer
                            relayList.set(index, false);
                            relayList.set(index2, false);
                            connectedList.set(index, true);
                            connectedList.set(index2, true);
                            connectedPeer(alias, clientA, outList.get(index));
                            connectedPeer(aliasB, inList.get(index2), clientB);
                        } else if(relayList.get(index)) {
                            String msg2 = alias + ":" + message;
                            System.out.println(msg2);
                            clientB.write(msg2.getBytes());     //Send message to peer
                            clientB.write('\n');
                            clientB.flush(); //empty buffer
                        }
                    }
                } catch (IOException e ) {
                    System.out.println("Lost connection with " + alias);
                    System.out.println(e);
                    relayList.set(index, false);
                    connectedList.set(index, true);
                    if(aliasList.contains(aliasB)){
                        relayList.set(index2, false);
                        connectedList.set(index2, true);
                    }
                    try {
                        connectedPeer(aliasB, inList.get(index2), clientB);
                    } catch (IOException e2 ) {
                        System.out.println(e2);
                    }
                    removeClient(index);
                }
            }
        }).start();
    }

    //Function to remove client
    private void removeClient(int index) {
        aliasList.remove(index);
        inList.remove(index);
        outList.remove(index);
        connectedList.remove(index);
        relayList.remove(index);
    }

    //Function to enter commands in server console to initiate shutdown
    private void serverCommands() {
        this.listen = new Thread(() -> {
            boolean connected = true;
            while (connected) {
                try {
                    Scanner myObj = new Scanner(System.in);
                    String msg = myObj.nextLine();
                    if("/exit".equals(msg)){
                        exit();
                        connected = false;
                    }
                } catch (Exception e) {
                    System.err.println(e);
                }
            }
        });
        this.listen.start();
    }


    //Functie om de server af te sl
    private void exit(){
        try {
            ListIterator<BufferedOutputStream> iterator = outList.listIterator(); //get Iterator of every connected peer
            while(iterator.hasNext()){
                BufferedOutputStream out = iterator.next();
                out.write("Server shutting down!".getBytes());     //Send message to peer
                out.write('\n');
                out.flush(); //empty buffer
            }
            exit = true;
            socketConnect.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
