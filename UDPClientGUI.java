package udpclientgui; 
import javax.swing.*; 
import java.awt.*; 
import java.net.*; 
 
public class UDPClientGUI { 
    private DatagramSocket socket; 
    private InetAddress serverAddress; 
    private int serverPort; 
    private volatile boolean running = true; 
    private JTextArea chatArea; 
    private JTextField messageField; 
    private int clientId = -1; // Client ID assigned by server 
    private JFrame frame; 
 
    public UDPClientGUI(String serverIP, int serverPort) throws Exception { 
        this.serverAddress = InetAddress.getByName(serverIP); 
        this.serverPort = serverPort; 
        this.socket = new DatagramSocket(); 
 
        createGUI(); 
        startReadThread(); 
    } 
 
    private void createGUI() { 
        frame = new JFrame("UDP Chat Client"); 
        frame.setSize(500, 400); 
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); 
        frame.setLayout(new BorderLayout()); 
 
        chatArea = new JTextArea(); 
        chatArea.setEditable(false); 
        frame.add(new JScrollPane(chatArea), BorderLayout.CENTER); 
 
        JPanel bottomPanel = new JPanel(new BorderLayout()); 
        messageField = new JTextField(); 
        JButton sendButton = new JButton("Send"); 
        sendButton.addActionListener(e -> sendMessage()); 
 
        bottomPanel.add(messageField, BorderLayout.CENTER); 
        bottomPanel.add(sendButton, BorderLayout.EAST); 
        frame.add(bottomPanel, BorderLayout.SOUTH); 
 
        frame.setVisible(true); 
    } 
 
    private void startReadThread() { 
        new Thread(() -> { 
            try { 
                byte[] receiveBuffer = new byte[1024]; 
                while (running) { 
                    DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, 
receiveBuffer.length); 
                    socket.receive(receivePacket); 
                    String message = new String(receivePacket.getData(), 0, receivePacket.getLength()); 
 
                    if (message.startsWith("Your Client ID:")) { 
                        clientId = Integer.parseInt(message.split(":")[1].trim()); 
                        chatArea.append("Assigned Client ID: " + clientId + "\n"); 
                    } else { 
                        chatArea.append(message + "\n"); 
                    } 
                } 
            } catch (Exception e) { 
                chatArea.append("Disconnected from server.\n"); 
            } finally { 
                closeClient(); 
            } 
        }).start(); 
    } 
 
    private void sendMessage() { 
        try { 
            String message = messageField.getText(); 
            if (message.isEmpty()) return; 
            messageField.setText(""); 
            byte[] sendData = message.getBytes(); 
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, 
serverAddress, serverPort); 
            socket.send(sendPacket); 
 
            if (message.equalsIgnoreCase("exit")) { 
                running = false; 
                socket.close(); 
                closeClient(); 
            } 
        } catch (Exception e) { 
            chatArea.append("Error sending message.\n"); 
        } 
    } 
 
    private void closeClient() { 
        running = false; 
        if (socket != null && !socket.isClosed()) { 
            socket.close(); 
        } 
        SwingUtilities.invokeLater(() -> { 
            frame.dispose(); // Close the GUI window when disconnected 
        }); 
    } 
 
    public static void main(String[] args) { 
        SwingUtilities.invokeLater(() -> { 
            try { 
                JPanel inputPanel = new JPanel(new GridLayout(3, 2)); 
                JTextField ipField = new JTextField("localhost"); 
                JTextField portField = new JTextField("5000"); 
 
                inputPanel.add(new JLabel("Server IP:")); 
                inputPanel.add(ipField); 
                inputPanel.add(new JLabel("Server Port:")); 
                inputPanel.add(portField); 
 
                int result = JOptionPane.showConfirmDialog(null, inputPanel, "Enter Server Details", 
                        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE); 
 
                if (result == JOptionPane.OK_OPTION) { 
                    String serverIP = ipField.getText(); 
                    int serverPort = Integer.parseInt(portField.getText()); 
                    new UDPClientGUI(serverIP, serverPort); 
                } 
            } catch (Exception e) { 
                JOptionPane.showMessageDialog(null, "Client error: " + e.getMessage(), "Error", 
JOptionPane.ERROR_MESSAGE); 
            } 
        }); 
    } 
} 
