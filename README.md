# TCPRelay
A client-server chat application using a server as relay in Java.

Server
To start the server, use the command "java ServerMediatorRelay.java" to start on default port 9000. To choose a different port add argument after like "java ServerMediatorRelay.java 10000".


Peer

To start the peer, type "java PeerRelay.java ALIAS" with ALIAS being your nickname. This will start the peer locally. To connect to a remote server add a server IP, port and alias as argument instead.

PeerRelay has 3 commands:
/alias - show list of aliasses connected
/connect ALIAS - connect to a target alias
/disconnect - disconnect from connected alias
