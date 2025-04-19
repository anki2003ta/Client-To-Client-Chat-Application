package udpservergui; 
import javax.swing.*; 
import java.awt.*; 
import java.net.*; 
import java.util.HashMap; 
import java.util.Map; 
 
public class UDPServerGUI { 
    private static final int SERVER_PORT = 5000; 
    private static Map<InetSocketAddress, Integer> clients = new HashMap<>(); 
    private static int clientIdCounter = 1; 
    private static JTextArea logArea; 
 
    public static void main(String[] args) { 
        SwingUtilities.invokeLater(() -> createGUI()); 
        startServer(); 
    } 
 
    private static void createGUI() { 
        JFrame frame = new JFrame("UDP Chat Server"); 
        frame.setSize(500, 400); 
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
        frame.setLayout(new BorderLayout()); 
 
        logArea = new JTextArea(); 
        logArea.setEditable(false); 
        frame.add(new JScrollPane(logArea), BorderLayout.CENTER); 
 
        frame.setVisible(true); 
    } 
 
    private static void startServer() { 
        new Thread(() -> { 
            try (DatagramSocket serverSocket = new DatagramSocket(SERVER_PORT)) { 
                log("Server started on port " + SERVER_PORT); 
 
                byte[] receiveBuffer = new byte[1024]; 
                while (true) { 
                    DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, 
receiveBuffer.length); 
                    serverSocket.receive(receivePacket); 
                    String message = new String(receivePacket.getData(), 0, receivePacket.getLength()); 
 
                    InetSocketAddress clientAddress = new InetSocketAddress( 
                            receivePacket.getAddress(), receivePacket.getPort()); 
 
                    if (!clients.containsKey(clientAddress)) { 
                        clients.put(clientAddress, clientIdCounter++); 
                        sendMessage(serverSocket, "Your Client ID: " + clients.get(clientAddress), 
clientAddress); 
                        log("New client connected. Assigned ID: " + clients.get(clientAddress)); 
                    } 
 
                    handleClientMessage(serverSocket, message, clients.get(clientAddress), clientAddress); 
                } 
            } catch (Exception e) { 
                log("Server error: " + e.getMessage()); 
            } 
        }).start(); 
    } 
 
    private static void handleClientMessage(DatagramSocket serverSocket, String message, int clientId, 
InetSocketAddress clientAddress) { 
        try { 
            if (message.equalsIgnoreCase("GET")) { 
                sendClientList(serverSocket, clientAddress); 
            } else if (message.startsWith("SEND")) { 
                sendMessageToClient(serverSocket, message, clientId); 
            } else if (message.equalsIgnoreCase("exit")) { 
                clients.remove(clientAddress); 
                log("Client " + clientId + " disconnected."); 
            } else { 
                sendMessage(serverSocket, "Invalid Command! Use 'GET' or 'SEND <client_id> <message>'", 
clientAddress); 
            } 
        } catch (Exception e) { 
            log("Error handling message: " + e.getMessage()); 
        } 
    } 
 
    private static void sendClientList(DatagramSocket serverSocket, InetSocketAddress clientAddress) { 
        StringBuilder clientList = new StringBuilder("Connected Clients: "); 
        for (Integer id : clients.values()) { 
            clientList.append(id).append(" "); 
        } 
        sendMessage(serverSocket, clientList.toString().trim(), clientAddress); 
    } 
 
    private static void sendMessageToClient(DatagramSocket serverSocket, String message, int 
senderId) { 
        String[] parts = message.split(" ", 3); 
        if (parts.length < 3) return; 
 
        try { 
            int recipientId = Integer.parseInt(parts[1]); 
            String msg = parts[2]; 
            for (Map.Entry<InetSocketAddress, Integer> entry : clients.entrySet()) { 
                if (entry.getValue() == recipientId) { 
                    sendMessage(serverSocket, "From Client " + senderId + ": " + msg, entry.getKey()); 
                    return; 
                } 
            } 
        } catch (NumberFormatException ignored) {} 
    } 
 
    private static void sendMessage(DatagramSocket serverSocket, String message, InetSocketAddress 
clientAddress) { 
        try { 
            byte[] sendData = message.getBytes(); 
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, 
                    clientAddress.getAddress(), clientAddress.getPort()); 
            serverSocket.send(sendPacket); 
        } catch (Exception e) { 
            log("Error sending message."); 
        } 
    } 
 
    private static void log(String message) { 
        SwingUtilities.invokeLater(() -> logArea.append(message + "\n")); 
    } 
}
