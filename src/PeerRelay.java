import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.Scanner; // import the Scanner class

public class PeerRelay {

    private static InetAddress mediatorIP;
    private static int mediatorTcpDiscussionPort;

    private Socket socketDiscussion;

    private BufferedReader inDiscussion;
    private BufferedOutputStream outDiscussion;

    private String message = "";
    private static String sendMessage = "";
    private volatile boolean runningHole;

    private Thread readOnHole, writeOnHole;

    //Constructor
    public PeerRelay(InetAddress ip, int tcpDiscussionPort) throws IOException {
        socketDiscussion = createSocket(ip, tcpDiscussionPort);

        this.runningHole = true;
        createInOutputStreamDiscussion(socketDiscussion);
        readOnHoleRelay();
        sendMessage(outDiscussion, sendMessage);
    }

    private Socket createSocket(InetAddress ip, int port) {
        try {
            Socket socket = new Socket(ip, port);
            return socket;
        } catch (IOException ex) {
            System.err.println("Exception creating a socket: " + ex);
        }
        return null;
    }

    private Socket createSocket() {
        System.out.println("attempt to create raw socket");
        Socket socket = new Socket();
        System.out.println("created socket: " + socket.getInetAddress());
        return socket;
    }


    private void closeSocket(Socket socket) {
        System.out.println("close socket");
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createInOutputStreamDiscussion(Socket discussionSocket) throws IOException {
        inDiscussion = new BufferedReader(new InputStreamReader(discussionSocket.getInputStream()));
        outDiscussion = new BufferedOutputStream(discussionSocket.getOutputStream());
    }


    //Function to send a message to a peer or server.
    private void sendMessage(BufferedOutputStream stream, String message) throws IOException {
        byte[] sendData = message.getBytes();
        stream.write(sendData);
        stream.write('\n');
        stream.flush();
    }


    //Function to receive messages from another peer through server using relay.
    private void readOnHoleRelay() throws IOException {
        this.readOnHole = new Thread(() -> {
            boolean connected = true;
            while (connected) {
                try {
                    message = inDiscussion.readLine();
                    if("connected".equals(message)) {
                        writeDataOnHoleRelay();
                    }
                    if(message != null) {
                        System.out.println(message);
                    }
                    if(message == null) {
                        closeSocket(socketDiscussion);
                        connected = false;
                    }
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        });
        this.readOnHole.start();
    }

    //Function to send messages to another peer through server using relay.
    private void writeDataOnHoleRelay() {
//        System.out.println("write data on hole relay");
        this.writeOnHole = new Thread(() -> {
            boolean connected = true;
            while (connected) {
                try {
                    Scanner myObj = new Scanner(System.in);
                    String msg = myObj.nextLine();
                    sendMessage(outDiscussion, msg);
                } catch (IOException e) {
                    System.out.println(e);
                    System.out.println("Lost connection!");
                    connected = false;

                } catch (Exception e) {
                    System.err.println("SleepException");
                    System.err.println(e);
                }
            }
        });
        this.writeOnHole.start();
    }


    //main method to check for args.
    public static void main(String[] args) throws IOException {
        if (args.length == 4) {
            sendMessage = args[2];
            clientSetup(InetAddress.getByName(args[0].trim()), Integer.parseInt(args[1].trim()));
        } else if (args.length == 3) {
            sendMessage = args[2];
            clientSetup(InetAddress.getByName(args[0].trim()), Integer.parseInt(args[1].trim()));
        } else if (args.length == 1) {
            sendMessage = args[0];
            clientSetup(InetAddress.getByName("127.0.0.1"), 9000);
        } else {
            clientSetup(InetAddress.getByName("127.0.0.1"), 9000);
        }
    }

    //Function to setup a client/peer
    private static void clientSetup(InetAddress clientIp, int discussionPort) {
        try {
            System.out.println("Client A running with IP: " + clientIp + " DiscussionPort: " + discussionPort);
            mediatorIP = clientIp;
            mediatorTcpDiscussionPort = discussionPort;
            new PeerRelay(mediatorIP, mediatorTcpDiscussionPort);
        } catch (Exception ex) {
            System.err.println("error with connecting to server, exiting");
            System.exit(0);
        }
    }
}
