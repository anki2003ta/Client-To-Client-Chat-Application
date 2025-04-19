package tcpclient1gui; 
import javax.swing.*; 
import java.awt.*; 
import java.io.*; 
import java.net.*; 
 
public class TCPClient1GUI { 
    private JTextArea chatArea; 
    private JTextField messageField; 
    private PrintWriter out; 
    private BufferedReader in; 
    private Socket socket; 
    private Thread readThread, writeThread; 
    private volatile boolean running = true;  // Flag to control thread execution 
 
    public TCPClient1GUI() { 
        JFrame frame = new JFrame("TCP Chat Client"); 
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
        frame.setSize(400, 400); 
 
        JPanel panel = new JPanel(new BorderLayout()); 
        chatArea = new JTextArea(); 
        chatArea.setEditable(false); 
        panel.add(new JScrollPane(chatArea), BorderLayout.CENTER); 
 
        JPanel inputPanel = new JPanel(new BorderLayout()); 
        messageField = new JTextField(); 
        JButton sendButton = new JButton("Send"); 
        inputPanel.add(messageField, BorderLayout.CENTER); 
        inputPanel.add(sendButton, BorderLayout.EAST); 
 
        panel.add(inputPanel, BorderLayout.SOUTH); 
        frame.add(panel); 
        frame.setVisible(true); 
 
        setupConnection(); 
 
        sendButton.addActionListener(e -> sendMessage()); 
        messageField.addActionListener(e -> sendMessage()); 
    } 
 
    private void setupConnection() { 
        String ip = JOptionPane.showInputDialog("Enter Server IP:", "localhost"); 
        String portString = JOptionPane.showInputDialog("Enter Server Port:", "5000"); 
 
        try { 
            int port = Integer.parseInt(portString); 
            socket = new Socket(ip, port); 
            in = new BufferedReader(new InputStreamReader(socket.getInputStream())); 
            out = new PrintWriter(socket.getOutputStream(), true); 
            chatArea.append("Connected to server\n"); 
 
            startReadThread(); 
            startWriteThread(); 
        } catch (IOException | NumberFormatException e) { 
            JOptionPane.showMessageDialog(null, "Connection failed: " + e.getMessage()); 
        } 
    } 
 
    private void startReadThread() { 
        readThread = new Thread(() -> { 
            try { 
                String serverMessage; 
                while (running && (serverMessage = in.readLine()) != null) { 
                    chatArea.append(serverMessage + "\n"); 
                } 
            } catch (IOException e) { 
                chatArea.append("Disconnected from server\n"); 
            } 
        }); 
        readThread.start(); 
    } 
 
    private void startWriteThread() { 
        writeThread = new Thread(() -> { 
            try { 
                while (running) { 
                    synchronized (messageField) { 
                        messageField.wait(); // Wait until a message is entered 
                    } 
                    String message = messageField.getText().trim(); 
                    if (!message.isEmpty()) { 
                        out.println(message); 
                        if (message.equalsIgnoreCase("exit")) { 
                            disconnect(); 
                            break; 
                        } 
                        messageField.setText(""); 
                    } 
                } 
            } catch (InterruptedException e) { 
                chatArea.append("Write thread interrupted.\n"); 
            } 
        }); 
        writeThread.start(); 
    } 
 
    private void sendMessage() { 
        synchronized (messageField) { 
            messageField.notify(); // Wake up the Write Thread to send the message 
        } 
    } 
 
    private void disconnect() { 
        running = false; 
try { 
socket.close(); 
} catch (IOException e) { 
chatArea.append("Error closing socket\n"); 
} 
System.exit(0); 
} 
public static void main(String[] args) { 
SwingUtilities.invokeLater(TCPClient1GUI::new); 
} 
}
