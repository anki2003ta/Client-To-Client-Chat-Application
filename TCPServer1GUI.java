package tcpserver1gui; 
import javax.swing.*; 
import java.awt.*; 
import java.io.*; 
import java.net.*; 
import java.util.*; 
 
public class TCPServer1GUI{ 
    private static int clientIdCounter = 1; 
    private static final Map<Integer, ClientHandler> clients = new HashMap<>(); 
    private static JTextArea textArea; 
    private static volatile boolean running = true;  // Flag to control server execution 
 
    public static void main(String[] args) { 
        JFrame frame = new JFrame("TCP Chat Server"); 
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
        frame.setSize(400, 300); 
 
        textArea = new JTextArea(); 
        textArea.setEditable(false); 
        frame.add(new JScrollPane(textArea), BorderLayout.CENTER); 
        frame.setVisible(true); 
 
        startServer(); 
    } 
 
    private static void startServer() { 
        new Thread(() -> { 
            try (ServerSocket serverSocket = new ServerSocket(5000)) { 
                log("Server started on port 5000..."); 
 
                while (running) { 
                    Socket clientSocket = serverSocket.accept(); 
                    ClientHandler handler = new ClientHandler(clientSocket, clientIdCounter); 
                    synchronized (clients) { 
                        clients.put(clientIdCounter, handler); 
                    } 
                    new Thread(handler).start(); 
                    log("Client " + clientIdCounter + " connected."); 
                    clientIdCounter++; 
                } 
            } catch (IOException e) { 
                log("Error: " + e.getMessage()); 
            } 
        }).start(); 
    } 
 
    private static void log(String message) { 
        SwingUtilities.invokeLater(() -> textArea.append(message + "\n")); 
    } 
 
    static class ClientHandler implements Runnable { 
        private Socket socket; 
        private int clientId; 
        private PrintWriter out; 
        private BufferedReader in; 
        private Thread readThread, writeThread; 
        private volatile boolean clientRunning = true;  // Flag to control client threads 
 
        public ClientHandler(Socket socket, int clientId) { 
            this.socket = socket; 
            this.clientId = clientId; 
        } 
 
        @Override 
        public void run() { 
            try { 
                in = new BufferedReader(new InputStreamReader(socket.getInputStream())); 
                out = new PrintWriter(socket.getOutputStream(), true); 
                out.println("Welcome! Your Client Id: " + clientId); 
 
                startReadThread(); 
                startWriteThread(); 
 
                // Wait for threads to finish 
                readThread.join(); 
                writeThread.join(); 
            } catch (IOException | InterruptedException e) { 
                log("Client " + clientId + " disconnected."); 
            } finally { 
                disconnectClient(); 
            } 
        } 
 
        private void startReadThread() { 
            readThread = new Thread(() -> { 
                try { 
                    String message; 
                    while (clientRunning && (message = in.readLine()) != null) { 
                        if (message.equalsIgnoreCase("GET")) { 
                            sendClientList(); 
                        } else if (message.startsWith("SEND")) { 
                            sendMessageToClient(message); 
                        } else if (message.equalsIgnoreCase("exit")) { 
                            disconnectClient(); 
                            break; 
                        } else { 
                            out.println("Invalid Command! Use 'GET' or 'SEND <client_id> <message>'"); 
                        } 
                    } 
                } catch (IOException e) { 
                    log("Client " + clientId + " disconnected."); 
                } 
            }); 
            readThread.start(); 
        } 
 
        private void startWriteThread() { 
            writeThread = new Thread(() -> { 
                try { 
                    while (clientRunning) { 
                        Thread.sleep(100); // Prevents CPU overuse 
                    } 
                } catch (InterruptedException e) { 
                    log("Write thread interrupted."); 
                } 
            }); 
            writeThread.start(); 
        } 
 
        private void sendClientList() { 
            StringBuilder clientList = new StringBuilder("Connected Clients: "); 
            synchronized (clients) { 
                for (Integer id : clients.keySet()) { 
                    clientList.append(id).append(" "); 
                } 
            } 
            out.println(clientList.toString().trim()); 
        } 
 
        private void sendMessageToClient(String message) { 
            String[] parts = message.split(" ", 3); 
            if (parts.length < 3) { 
                out.println("Invalid SEND format. Use: SEND <client_id> <message>"); 
                return; 
            } 
            try { 
                int recipientId = Integer.parseInt(parts[1]); 
                String msg = parts[2]; 
                synchronized (clients) { 
                    if (clients.containsKey(recipientId)) { 
                        clients.get(recipientId).out.println("From Client " + clientId + ": " + msg); 
                        log("Client " + clientId + " sent to Client " + recipientId + ": " + msg); 
                    } else { 
                        out.println("Client " + recipientId + " not found."); 
                    } 
                } 
            } catch (NumberFormatException e) { 
                out.println("Invalid Client ID"); 
            } 
        } 
 
        private void disconnectClient() { 
            clientRunning = false; 
            try { 
                socket.close(); 
                synchronized (clients) { 
                    clients.remove(clientId); 
                } 
                log("Client " + clientId + " disconnected."); 
            } catch (IOException e) { 
                log("Error closing connection: " + e.getMessage()); 
            } 
        } 
    }
}
