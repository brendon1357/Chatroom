import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.text.DefaultEditorKit;

/*
 * Client class will handle client-side services
 */
public class Client extends JFrame {
	private JPanel separationPanel;
	private JLabel offlineLabel;
	private JButton loginButton;
	private JTextField usernameTextField, passwordTextField;
    private BufferedReader in;
    private PrintWriter out;
    private String username, password;
    private SSLSocket sslSocket;
    private static final int PORT = 9999;
    
    public Client() throws IOException {
    	super();
    	
    	BufferedImage buttonPic = ImageIO.read(this.getClass().getResourceAsStream("resources/Play.png"));
    	BufferedImage iconPic = ImageIO.read(this.getClass().getResourceAsStream("resources/personIcon.png"));
    	JLabel picLabel = new JLabel(new ImageIcon(iconPic));
    	
    	loginButton = new JButton("Login");
    	loginButton.setHorizontalTextPosition(JButton.LEFT);
    	loginButton.setIconTextGap(10);
    	loginButton.setBackground(Color.decode("#1e81b0"));
    	loginButton.setForeground(Color.WHITE);
    	loginButton.setBorderPainted(false);
    	loginButton.setFont(new Font("Arial", Font.BOLD, 16));
    	loginButton.setPreferredSize(new Dimension(120, 30));
    	loginButton.setIcon(new ImageIcon(buttonPic));
    	loginButton.setOpaque(true);
    	loginButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
    	loginButton.addMouseListener(new java.awt.event.MouseAdapter() {
    	    public void mouseEntered(java.awt.event.MouseEvent evt) {
    	    	loginButton.setBackground(Color.decode("#58add5"));
    	    }

    	    public void mouseExited(java.awt.event.MouseEvent evt) {
    	    	loginButton.setBackground(Color.decode("#1e81b0"));
    	    }
    	});
    	loginButton.addActionListener(ev -> {
			try {
				login();
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
			} 
		});
    	
    	// User must enter a username
    	JLabel usernameLabel = new JLabel("Username");
    	usernameTextField = new JTextField();
    	usernameTextField.setFont(new Font("Arial", Font.PLAIN, 16));
    	usernameTextField.setPreferredSize(new Dimension(50, 22));
    	usernameTextField.setBorder(BorderFactory.createLineBorder(Color.BLACK));
    	
    	// User must enter a password
    	JLabel passwordLabel = new JLabel("Chatroom Password");
    	passwordTextField = new JPasswordField();
    	passwordTextField.setFont(new Font("Arial", Font.PLAIN, 16));
    	passwordTextField.setPreferredSize(new Dimension(50, 22));
    	passwordTextField.setBorder(BorderFactory.createLineBorder(Color.BLACK));
    	
    	JPanel loginPanel = new JPanel();
    	loginPanel.setLayout(new BoxLayout(loginPanel, BoxLayout.PAGE_AXIS));
    	
    	loginPanel.add(usernameLabel);
    	loginPanel.add(usernameTextField);
    	loginPanel.add(Box.createVerticalStrut(10));
    	loginPanel.add(passwordLabel);
    	loginPanel.add(passwordTextField);
    	loginPanel.setOpaque(false);
    	
    	JPanel buttonPanel = new JPanel();
    	buttonPanel.setLayout(new FlowLayout());
    	buttonPanel.add(loginButton);
    	buttonPanel.setOpaque(false);
    	
    	JPanel leftPanel = new JPanel();
    	leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.PAGE_AXIS));
    	leftPanel.setOpaque(false);
    	leftPanel.add(picLabel);
    	
    	JLabel welcomeLabel = new JLabel("Sign In", JLabel.CENTER);
    	welcomeLabel.setFont(new Font("Helvetica", Font.BOLD, 28));
    	
    	JPanel welcomeContainer = new JPanel(new FlowLayout());
    	welcomeContainer.setOpaque(false);
    	welcomeContainer.add(welcomeLabel);
    	
    	JPanel loginContainer = new JPanel();
    	loginContainer.setLayout(new BoxLayout(loginContainer, BoxLayout.PAGE_AXIS));
    	loginContainer.setOpaque(false);
    	loginContainer.add(welcomeContainer);  	
    	loginContainer.add(Box.createVerticalStrut(20));
    	loginContainer.add(loginPanel);
    	loginContainer.add(Box.createVerticalStrut(30));
    	loginContainer.add(buttonPanel);
    	
    	offlineLabel = new JLabel("Server is currently offline");
    	Font font = new Font("Arial", Font.BOLD, 16);
		offlineLabel.setFont(font);
		offlineLabel.setForeground(Color.WHITE);
    	JPanel offlinePanel = new JPanel(new FlowLayout());
    	offlinePanel.add(Box.createVerticalGlue());
    	offlinePanel.add(offlineLabel);
    	offlinePanel.add(Box.createVerticalGlue());
    	offlinePanel.setOpaque(false);
    	offlineLabel.setHorizontalAlignment(JLabel.LEFT);
    	
    	JPanel rightPanel = new JPanel();
    	rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.PAGE_AXIS));
    	rightPanel.setBackground(Color.WHITE);    	
    	rightPanel.setBorder(BorderFactory.createEmptyBorder(10, 40, 0, 40));
    	rightPanel.add(Box.createVerticalGlue());
    	rightPanel.add(loginContainer);
    	rightPanel.add(Box.createVerticalGlue());
    	rightPanel.add(offlinePanel);
	
    	separationPanel = new JPanel();
    	separationPanel.setBackground(Color.decode("#1e81b0"));
    	separationPanel.setLayout(new BoxLayout(separationPanel, BoxLayout.X_AXIS));
    	separationPanel.add(Box.createHorizontalStrut(60));
    	separationPanel.add(leftPanel);
    	separationPanel.add(Box.createHorizontalStrut(60));
    	separationPanel.add(rightPanel);
    }
    
    /*
     * Construct the GUI, pack the components and set it visible
     */
    private void createAndShowLoginGUI() {
    	try{
 		   UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
 	 	}
 		catch(Exception e){
 			  e.printStackTrace(); 
 		}
    	if (System.getProperty("os.name", "").toUpperCase().startsWith("MAC")) {
    		InputMap im = (InputMap) UIManager.get("TextField.focusInputMap");
    		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.META_DOWN_MASK), DefaultEditorKit.copyAction);
    		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.META_DOWN_MASK), DefaultEditorKit.pasteAction);
    		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.META_DOWN_MASK), DefaultEditorKit.cutAction);
    	}    	
    	getContentPane().add(separationPanel);
    	pack();
    	
    	Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
    	setLocation(dim.width / 2 - this.getSize().width / 2, dim.height / 2 - this.getSize().height / 2);
    	setTitle("Login");
    	setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    	setResizable(false);
    	setVisible(true);   	
    	getRootPane().setDefaultButton(loginButton);
    }
    
    // Simple getter to get the client's username
    public String getUsername() {
    	return username;
    }
    
    /*
     * Closing all buffers and socket connection
     */
    private static void closeEverything(SSLSocket socket, BufferedReader in, PrintWriter out) {
        try {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /*
     * Used to send messages to the server
     */
    private void output(String message) {
		out.println(message);
		out.flush();
    }
    
    /*
     * Validate the password given by the user and open
     * chat window if the password is valid
     */
    private void login() throws IOException, ClassNotFoundException {
        try {
            SSLSocketFactory sslSocketFactory = (SSLSocketFactory)SSLSocketFactory.getDefault();
            sslSocket = (SSLSocket)sslSocketFactory.createSocket("localhost", 33333);
			this.out = new PrintWriter(new OutputStreamWriter(sslSocket.getOutputStream()));
            this.in = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));
        } catch (IOException e) {
            closeEverything(sslSocket, in, out);
        }
        
    	if (in == null) {
    		offlineLabel.setForeground(Color.RED);
    	}
    	else {
    		offlineLabel.setForeground(Color.WHITE);
    		username = usernameTextField.getText();
        	password = passwordTextField.getText();
        	
    		output(username);
        	output("#PASSWORD: " + password);	
        	String response = null;
        	// Response from server
        	try {
        		response = in.readLine();
            	System.out.println(response);
            	// If the server acknowledges that password is valid
            	if (response.equals("Successful Connection")) {
            		ArrayList<String> nameList = new ArrayList<>();
            		String names = in.readLine();
            		String parsedNames = names.replaceAll("\\[", "").replaceAll("\\]", "").replaceAll(",", "");
            		String[] split = parsedNames.split(" ");
            		for (String name: split) {
            			nameList.add(name);
            		}
            		// Dispose login window when logged in
        			this.dispose();
                	ChatWindow chatWindow = new ChatWindow(this, sslSocket, in, out, nameList);
                	chatWindow.listenForMessage();
                	chatWindow.createAndShowChatGUI();            	
            	}
            	else {
            		// disconnect client if they provide invalid input
            		closeEverything(sslSocket, in, out);
            		if (response.equals("Failed Connection")) {
               		 JOptionPane.showMessageDialog(null, "Invalid username or password. Username\nmust not contain spaces.",
                                "Error", JOptionPane.ERROR_MESSAGE);
            		}
    		       	else if (response.equals("Failed Connection Username Exists")) {
    		       		JOptionPane.showMessageDialog(null, "Provided username already exists.\nPlease enter a new username.",
    		                       "Error", JOptionPane.ERROR_MESSAGE);
    		       	}
    		       	else if (response.equals("Failed Connection Username Too Long")) {
    		       		JOptionPane.showMessageDialog(null, "Provided username is too long. Username must\nbe 25 characters or less",
    		                       "Error", JOptionPane.ERROR_MESSAGE);
    		       	}
    		       	else if (response.equals("Failed Connection Input Empty")) {
    		       		JOptionPane.showMessageDialog(null, "Username and password must not be empty.",
    		                       "Warning", JOptionPane.WARNING_MESSAGE);
    		       	}
            	}
        	} catch (Exception e) {
        		e.printStackTrace();
        		offlineLabel.setForeground(Color.RED);
        	}
        	
    	}		
    }
    
    /*
     * Bind a server socket so that an exception will be thrown if user tries to open more than one 
     * instance of the program
     */
    private static void checkIfRunning() {
    	  try {
    	    ServerSocket serverSocket = new ServerSocket(PORT, 0, InetAddress.getByAddress(new byte[] {127,0,0,1}));
    	  }
    	  catch (BindException e) {
    	    System.err.println("Already running.");
    	    System.exit(1);
    	  }
    	  catch (IOException e) {
    	    System.err.println("Unexpected error.");
    	    e.printStackTrace();
    	    System.exit(2);
    	  }
	}
    
    public static void main(String[] args) throws IOException {
    	System.setProperty("javax.net.ssl.trustStore","PathToTrustStore");
        System.setProperty("javax.net.ssl.trustStorePassword","TrustStorePassword");
		Client client = new Client();
    	client.createAndShowLoginGUI();
    	// Prevents multiple instances of program running on a single machine
    	//checkIfRunning();
    }
}
