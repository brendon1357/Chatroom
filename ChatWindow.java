import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.net.ssl.SSLSocket;
import javax.swing.AbstractAction;
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
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.Timer;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.BoxView;
import javax.swing.text.ComponentView;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Element;
import javax.swing.text.IconView;
import javax.swing.text.LabelView;
import javax.swing.text.ParagraphView;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

/*
 * ChatWindow is the main UI where the client can read and send messages
 */
public class ChatWindow extends JFrame {
    private JButton sendButton;
    private Client client;
    private SSLSocket sslSocket;
    private BufferedReader in;
    private PrintWriter out;
    private JTextField inputField;
    private JTextPane textPane;
    private JPanel sendPanel, mainPanel, replyLabelPanel;
    private JLabel replyLabel, cancelLabel;
    private SimpleDateFormat formatter;
    private LightScrollPane scrollPane;
    private Date date;
    private ArrayList<String> namesList;
    private Timer timer;
    private String globalReplyMsg, globalReplyUsername;
    private boolean replying, longTimeOut, shortTimeOut;
    private int duration;
    private long lastTimeSent;
    
    public ChatWindow(Client client, SSLSocket sslSocket, BufferedReader in, PrintWriter out, ArrayList<String> namesList) throws IOException {
        super();
        this.client = client;
        this.sslSocket = sslSocket;
        this.in = in ;
        this.out = out;
        this.namesList = namesList;
        lastTimeSent = 0;
        duration = 1000;
        replying = false;
        longTimeOut = false;
        shortTimeOut = false;
        
        BufferedImage sendButtonPic = ImageIO.read(this.getClass().getResourceAsStream("resources/logo-4.png"));          
        formatter = new SimpleDateFormat("h:mm a");
        
        // Where the user will type their messages
        inputField = new JTextField(20);
        inputField.setForeground(Color.WHITE);
        inputField.setBackground(Color.decode("#23272a"));
        inputField.setBorder(null);
        inputField.setCaretColor(Color.WHITE);
        
        Font inputFont = inputField.getFont().deriveFont(Font.PLAIN, 20f);
        inputField.setFont(inputFont);

        // Where the user can view their sent messages and messages from other users
        textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setEditorKit(new WrapEditorKit());
        textPane.setBackground(Color.decode("#3F3F3F"));
        textPane.addMouseListener(new MouseAdapter() {
        	@Override
        	public void mouseClicked(MouseEvent me) {
        		StyledDocument doc = textPane.getStyledDocument();
        		Element elem = doc.getCharacterElement(textPane.viewToModel2D(me.getPoint()));
        		AttributeSet as = elem.getAttributes();
        		URLLinkAction urlLink = (URLLinkAction)as.getAttribute("urlact");
        		if (urlLink != null) {
        			urlLink.execute();
        		}
        	}
        });
        
        textPane.addMouseMotionListener(new MouseAdapter() {
        	@Override
        	public void mouseMoved(MouseEvent me) {
        		StyledDocument doc = textPane.getStyledDocument();
        		Element elem = doc.getCharacterElement(textPane.viewToModel2D(me.getPoint()));
        		AttributeSet as = elem.getAttributes();
        		URLLinkAction urlLink = (URLLinkAction)as.getAttribute("urlact");
    			if (urlLink != null) {
    				textPane.setCursor(new Cursor(Cursor.HAND_CURSOR)); 
    			}
    			else {
    				textPane.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    			}
        	}
        });
    	scrollPane = new LightScrollPane(textPane);
    	scrollPane.setPreferredSize(new Dimension(1000, 550));
        scrollPane.setBorder(null);
        
        // Hold the textpane where all messages/interactions will be displayed
        JPanel textPanePanel = new JPanel();
        textPanePanel.setLayout(new BoxLayout(textPanePanel, BoxLayout.PAGE_AXIS));
        textPanePanel.add(scrollPane);
        textPanePanel.add(Box.createVerticalStrut(10));
        textPanePanel.setOpaque(false);

        sendButton = new JButton(new ImageIcon(sendButtonPic));
        sendButton.setBorder(BorderFactory.createEmptyBorder());
        sendButton.setContentAreaFilled(false);
        sendButton.addActionListener(ev -> {
			send();
		});
        sendButton.setBorderPainted(false);
        sendButton.setFocusPainted(false);
        sendButton.setRolloverIcon(new ImageIcon("resources/logo-5-3.png"));
    	sendButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
    	sendButton.addMouseListener(new java.awt.event.MouseAdapter() {
    	    public void mouseEntered(java.awt.event.MouseEvent evt) {
    	    	sendButton.setBackground(Color.decode("#4d86b4"));
    	    }

    	    public void mouseExited(java.awt.event.MouseEvent evt) {
    	    	sendButton.setBackground(Color.decode("#154c79"));
    	    }
    	});
    	
    	// After the given duration, enable the send button
        timer = new Timer(duration, new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                sendButton.setEnabled(true);
            }
        });
        timer.setRepeats(false);
        
        replyLabel = new JLabel();
        replyLabel.setForeground(Color.WHITE);
        
        cancelLabel = new JLabel();
        cancelLabel.setForeground(Color.RED);
        cancelLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cancelLabel.addMouseListener(new MouseAdapter() {
        	@Override
        	public void mouseClicked(MouseEvent me) {
        		replying = false;
        		replyLabel.setText("");
        		cancelLabel.setText("");
        	}
        	
        	public void mouseEntered(MouseEvent me ) {
        		cancelLabel.setForeground(Color.decode("#e31202"));
        	}
        	
        	public void mouseExited(MouseEvent me) {
        		cancelLabel.setForeground(Color.decode("#ff2414"));
        	}
        });
        
        replyLabelPanel = new JPanel();
        replyLabelPanel.setLayout(new BoxLayout(replyLabelPanel, BoxLayout.X_AXIS));
        replyLabelPanel.setBackground(Color.decode("#3F3F3F"));
        replyLabelPanel.add(replyLabel);
        replyLabelPanel.add(cancelLabel);
        
        // Holding the send button and input field side by side
        sendPanel = new JPanel();
        sendPanel.setLayout(new BoxLayout(sendPanel, BoxLayout.X_AXIS));
        sendPanel.add(inputField);
        sendPanel.add(Box.createHorizontalStrut(10));
        sendPanel.add(sendButton);
        sendPanel.setOpaque(false);
        
        // Using grid panel with opaque here so that the reply label will start on the left side and
        // be able to extend full length of window
        JPanel gridPanel = new JPanel(new GridLayout());
        gridPanel.add(replyLabelPanel);
        gridPanel.setOpaque(false);
        
        JPanel containerPanel = new JPanel();
        containerPanel.setLayout(new BoxLayout(containerPanel, BoxLayout.PAGE_AXIS));
        containerPanel.add(gridPanel);
        containerPanel.add(sendPanel);
        containerPanel.setOpaque(false);

        // Border panel allows the window to resize nicely
        JPanel borderPanel = new JPanel(new BorderLayout());
        borderPanel.add(textPanePanel, BorderLayout.CENTER);
        borderPanel.add(containerPanel, BorderLayout.SOUTH);
        borderPanel.setOpaque(false);

        mainPanel = new JPanel();
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
        mainPanel.setBackground(Color.decode("#3F3F3F"));
        mainPanel.add(borderPanel);
    }

    /*
     * Construct the GUI, pack the components and set it visible
     */
    public void createAndShowChatGUI() {
    	// Setting the look and feel to be cross platform
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Binding the cmd key on mac for copy, cut, paste instead of the ctrl key
        if (System.getProperty("os.name", "").toUpperCase().startsWith("MAC")) {
    		InputMap im = textPane.getInputMap();
    		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.META_DOWN_MASK), DefaultEditorKit.copyAction);
    		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.META_DOWN_MASK), DefaultEditorKit.pasteAction);
    		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.META_DOWN_MASK), DefaultEditorKit.cutAction);
    	}
        // Add the main panel and call pack so components are placed properly in the window
        getContentPane().add(mainPanel);
        pack();
        
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        // Open in the middle of the screen
        setLocation(dim.width / 2 - this.getSize().width / 2, dim.height / 2 - this.getSize().height / 2);
        setTitle("Chat");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);
        setMinimumSize(new Dimension(675, 600));
        setVisible(true);
        // Defauly button is the enter key, line below allows user to send a message with the enter key
        getRootPane().setDefaultButton(sendButton);
        
        // Add some introductory text to the pane and display the names of the users who are currently connected
        appendToPane(textPane, "Welcome to the room! Type messages and send them to other connected users.", Color.decode("#F2F2F2"), false, 18, "SansSerif");
        appendToPane(textPane, "\n\nCurrently Connected: ", Color.decode("#E2E2E2"), false, 18, "SansSerif");
        
        // Loop through list of names and add them to the pane after the "Currently Connected: " message in list format
        // separated by comma
        for (int i = 0; i < namesList.size(); i++) {
        	if (i == namesList.size() - 1) {
        		appendToPane(textPane, namesList.get(i), Color.decode("#E2E2E2"), false, 18, "SansSerif");
        	}
        	else {
        		appendToPane(textPane, namesList.get(i) + ", ", Color.decode("#E2E2E2"), false, 18, "SansSerif");
        	}
        }
    }
    
    /*
     * Used to append text to the textpane.
     */
    public void appendToPane(JTextPane tp, String msg, Color c, boolean bold, int fontSize, String font) {
        StyledDocument doc = tp.getStyledDocument();
        Style style = tp.addStyle("Color Style", null);
        StyleConstants.setForeground(style, c);
        StyleConstants.setFontFamily(style, font);
        if (bold) {
            StyleConstants.setBold(style, true);
        }
    	StyleConstants.setFontSize(style, fontSize);
        try {
            doc.insertString(doc.getLength(), msg, style);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    /*
     * Close all buffers and connected socket
     */
    public void closeEverything(SSLSocket socket, BufferedReader in, PrintWriter out) {
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
     * Send out the message to be broadcasted to all other connected users
     */
    public void sendMessageOverSocket(String messageToSend, boolean replying, String replyUsername, String replyMsg) {
        out.println(client.getUsername());
        out.flush();
        if (replying) {
        	// Indicate that the message being send was a reply and not just a standard message
        	out.println("REPLYMESSAGEFROMCLIENT" + client.getUsername() + " " + messageToSend);
            out.flush();
            out.println(replyUsername + " " + replyMsg);
            out.flush();
        }
        else {
        	out.println("MESSAGEFROMCLIENT" + client.getUsername() + " " + messageToSend);
            out.flush();
        }
    }
    
    /*
     * Insert given number of newline characters in textPane
     */
    public void insertNewLines(int numOfLines, StyledDocument doc) {
    	try {
			doc.insertString(doc.getLength(), "\n".repeat(numOfLines), null);
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
    }
    
    /*
     * Kick client from the server, close connection between server and client
     */
    public void kickClient(StyledDocument doc) {
    	closeEverything(sslSocket, in , out);
    	insertNewLines(2, doc);
        appendToPane(textPane, "You have been kicked from the server!", Color.RED, true, 18, "Arial");
        sendButton.setEnabled(false);
    }
    
    /*
     * Appends message to text pane with a message sent from the server/admin
     */
    public void sendMessageFromAdmin(String msg, StyledDocument doc) {
    	JLabel username = new JLabel("Admin");
        username.setFont(new Font("Arial", Font.BOLD, 18));
        username.setForeground(Color.RED);
        insertNewLines(2, doc);
		textPane.setCaretPosition(textPane.getDocument().getLength());
		textPane.insertComponent(username);
		username.setAlignmentY(0.75f);
		appendToPane(textPane, "   " + formatDate(formatter.format(date)) + "\n", Color.LIGHT_GRAY, false, 12, "Helvetica");
		appendToPane(textPane, msg.replaceAll("SAY ", "").trim(), Color.decode("#F2F2F2"), false, 18, "Arial");
    }
    
    /*
     * Mute client for given amount of time which can be parsed from the provided message
     */
    public void muteClient(String msg, StyledDocument doc) {
    	int timeToMute;
		for (String name : namesList) {
			if (msg.contains(name)) {
				try {
					// Extract the time to mute the client from the given message
					timeToMute = Integer.parseInt(msg.replaceAll("MUTE ", "").replaceAll(name + " ", ""));
					if (name.equals(client.getUsername())) {
						JLabel muteNotification = new JLabel("You have been muted for " + timeToMute + " second(s)");
						muteNotification.setFont(new Font("Arial", Font.BOLD, 18));
                        muteNotification.setForeground(Color.RED);
                        insertNewLines(2, doc);
                        textPane.setCaretPosition(textPane.getDocument().getLength());
            			textPane.insertComponent(muteNotification);
            			muteNotification.setAlignmentY(0.75f);
						sendButton.setEnabled(false);
						// After the clients timeout has finished enable the send button again
						Runnable mute = new Runnable() {
					        	public void run() {
					        		sendButton.setEnabled(true);
					        	}
							};
				        // Mute client for provided time
				        ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
				        executor.schedule(mute, timeToMute, TimeUnit.SECONDS);                          						
    					break;
					} 
				// Catching exceptions in case of misinput on server side
				}catch (NumberFormatException e){
					e.printStackTrace();
				} catch (NullPointerException e) {
					e.printStackTrace();
				}
			}
		}
    }
    
    /*
     * Returns a list with all URLs contained in the input string
     */
    public static List<String> extractUrls(String text)
    {
        List<String> containedUrls = new ArrayList<String>();
        // Regex used to check for URLs in given text
        String urlRegex = "((https?|ftp|gopher|telnet|file):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)";
        Pattern pattern = Pattern.compile(urlRegex, Pattern.CASE_INSENSITIVE);
        Matcher urlMatcher = pattern.matcher(text);

        while (urlMatcher.find())
        {
            containedUrls.add(text.substring(urlMatcher.start(0),
                    urlMatcher.end(0)));
        }
        return containedUrls;
    }
    
    /*
     * Return true or false depending on whether or not given message has a URL in it
     */
    public boolean checkIfURLS(String message) {
    	if (extractUrls(message).size() > 0) {
    		return true;
    	}
    	return false;
    }
    
    /* FIXME: Huge mess, clean up code? Find better way to implement method, clean up conditional statements
     * or document thoroughly to increase readability
     * 
     * Specific method used to display messages in textPane that contain URLS so that they can be clicked and launched
     * in default web browser
     */
    public void sendMsgWithURLS(StyledDocument doc, String message, Color color) {
    	String original = message.trim();
    	List<String> urls = extractUrls(original);
    	// Making URLs underlined with a blue colour
		Style regularBlue = doc.addStyle("regularBlue", null);
		StyleConstants.setForeground(regularBlue, Color.decode("#6AC5FF"));
		StyleConstants.setUnderline(regularBlue,true);
		StyleConstants.setFontFamily(regularBlue, "Arial");
		StyleConstants.setFontSize(regularBlue, 18);
		String url, beforeURL, afterURL = "";
		for (int i = 0; i < urls.size(); i++) {
			// If we're on the last URL
    		if (i == urls.size() - 1) {
    			// Get the message before the URL
    			beforeURL = original.substring(0, original.indexOf(urls.get(i))).trim();
        		url = urls.get(i).trim();
        		// Adding this attribute allows code to be executed that takes the user to the URL web location when clicked
	    		regularBlue.addAttribute("urlact", new URLLinkAction(url));
	    		// Get the message after the URL
        		afterURL = original.substring(original.indexOf(urls.get(i)) + urls.get(i).length()).trim();
        		// If the last URL is the only URL in the list
    			if (i == 0) {
    				// If there is no message before the URL, don't add a space
    				if (beforeURL.equals("")) {
    					appendToPane(textPane, "\n" + beforeURL, color, false, 18, "Arial"); 
    				}
    				// Add a space if there is a message before the URL
    				else {
    					appendToPane(textPane, "\n" + beforeURL + " ", color, false, 18, "Arial"); 
    				}
	        		try {
						doc.insertString(doc.getLength(), url, regularBlue);
					} catch (BadLocationException e) {
						e.printStackTrace();
					}
	        		// If there is no message after the URL, don't add a space
	        		if (afterURL.equals("")) {
	        			appendToPane(textPane, afterURL + "   ", color, false, 18, "Arial");
	        		}
	        		// If there is a message after the URL, add a space
	        		else {
	        			appendToPane(textPane, " " + afterURL + "   ", color, false, 18, "Arial"); 
	        		}
    			}
    			else {
    				if (beforeURL.equals("")) {
    					appendToPane(textPane, beforeURL + " ", color, false, 18, "Arial"); 
    				}
    				else {
    					appendToPane(textPane, " " + beforeURL + " ", color, false, 18, "Arial"); 
    				}
    				
	        		try {
						doc.insertString(doc.getLength(), url, regularBlue);
					} catch (BadLocationException e) {
						e.printStackTrace();
					}
	        		if (afterURL.equals("")) {
	        			appendToPane(textPane, afterURL + "   ", color, false, 18, "Arial");
	        		}
	        		else {
	        			appendToPane(textPane, " " + afterURL + "   ", color, false, 18, "Arial"); 
	        		}
    			}
    		}
    		// Not on the last URL
    		else {
				beforeURL = original.substring(0, original.indexOf(urls.get(i))).trim();
        		url = urls.get(i).trim();
	    		regularBlue.addAttribute("urlact", new URLLinkAction(url));
	    		if (i == 0) {
	    			if (beforeURL.equals("")) {
    					appendToPane(textPane, "\n" + beforeURL, Color.decode("#E2E2E2"), false, 18, "Arial"); 
    				}
    				else {
    					appendToPane(textPane,  "\n" + beforeURL + " ", Color.decode("#E2E2E2"), false, 18, "Arial"); 
    				} 
	        		try {
						doc.insertString(doc.getLength(), url, regularBlue);
					} catch (BadLocationException e) {
						e.printStackTrace();
					}
    			}
	    		else {
					appendToPane(textPane, beforeURL + " ", Color.decode("#E2E2E2"), false, 18, "Arial"); 
	        		try {
						doc.insertString(doc.getLength(), url, regularBlue);
					} catch (BadLocationException e) {
						e.printStackTrace();
					}
	    		}
    		}
    		// Replace the before message in the original string and the URL as we loop through the URL list and keep going
    		// until there are no more URLs
    		String replaceFirst = original.substring(0, original.indexOf(urls.get(i))).trim();
    		String replaceSecond = urls.get(i).trim();
    		original = original.replaceFirst(replaceFirst, "").replaceFirst(replaceSecond, "");
    	}
    }
    
    /*
     * Large method responsible for displaying message related data in textPane 
     * (usernames, messages, replies, etc.)
     */
    public void handleDisplayingMessages(String username, StyledDocument doc, String message, String repliedUsername, String repliedMessage) {
    	JLabel usernameLabel = new JLabel(username);
    	usernameLabel.setForeground(Color.WHITE);
    	usernameLabel.setFont(new Font("Arial", Font.BOLD, 18));       
    	usernameLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));              
    	usernameLabel.addMouseListener(new MouseAdapter(){
    	   public void mouseClicked(MouseEvent me){
    		   inputField.setText(usernameLabel.getText());
    	   }
    	   
    	   public void mouseEntered(MouseEvent me) {
    		   usernameLabel.setForeground(Color.decode("#DFDFDF"));
    	   }
    	   
    	   public void mouseExited(MouseEvent me) {
    		   usernameLabel.setForeground(Color.WHITE);
    	   }
    	});
        JLabel reply = new JLabel("Reply");
        reply.addMouseListener(new MouseAdapter() {
        	@Override
        	public void mouseClicked(MouseEvent me) {
        		// Gets the rectangle around the label
        		Rectangle rect = reply.getParent().getBounds();
        		// Can access a point using the rectangle retrieved above
        	    Point pt = rect.getLocation();
        	    // Convert the point to a single integer value on the textpane
        		int viewToModel = textPane.viewToModel2D(pt);                		
        		// Get the entire paragraph at the position provided
        		Element paragraph = doc.getParagraphElement(viewToModel);
        		// Starting offset, ending offset
        		int start = paragraph.getStartOffset();
				int end = paragraph.getEndOffset();
				// Hardcoded values give position needed to get the JLabel for user's name.
				int x = 3;
				int y = 7;              		
        		Component c;
        		// Getting the component which is the user's name as a JLabel
				try {
					c = textPane.getComponentAt(x, (int)(textPane.modelToView2D(start).getY() - y));
					JLabel label;
            		if(c != null) {
                        Component[] inner = ((Container)c).getComponents();
                        for(int n = 0; n < inner.length; n++)
                        {
                            if(inner[n] instanceof JLabel)
                            {
                                label = (JLabel)inner[n];
                                // Extracting the String value from the user's name as a JLabel
                                globalReplyUsername = label.getText();                                   			
             					try {
             						// Get the entire message using the start offset, and length (end offset - start offset)
             						String msg = textPane.getText(start, end - start).trim();
        							if (msg.length () < 60) {
        								globalReplyMsg = msg.substring(0, msg.length());
        								replyLabel.setText(("Replying to " + globalReplyUsername + " \"" + globalReplyMsg + "\"   "));
        								cancelLabel.setText("Cancel");
        							}
        							else {
        								globalReplyMsg = msg.substring(0, 60).trim() + "...";
        								replyLabel.setText(("Replying to " + globalReplyUsername + " \"" + globalReplyMsg + "\"   "));
        								cancelLabel.setText("Cancel");
        							}
        							replying = true;
        						} catch (BadLocationException e) {
        							e.printStackTrace();
        						}   					
                            }
                        }
                    }
				} catch (BadLocationException e) {
					e.printStackTrace();
				}
        	}
        	
        	public void mouseEntered(MouseEvent me) {
        		reply.setForeground(Color.decode("#9F9F9F"));
        	}
        	
        	public void mouseExited(MouseEvent me) {
        		reply.setForeground(Color.LIGHT_GRAY);
        	}
        });
        insertNewLines(2, doc);
        textPane.setCaretPosition(textPane.getDocument().getLength());
		textPane.insertComponent(usernameLabel);
		usernameLabel.setAlignmentY(0.75f);
		appendToPane(textPane, "   " + formatDate(formatter.format(date)), Color.LIGHT_GRAY, false, 12, "Helvetica");
		// If this client is the one being replied to
		if (client.getUsername().equals(repliedUsername)) {
			// Either the message contains URLs
			if (checkIfURLS(message.trim())) {
				sendMsgWithURLS(doc, message.trim(), Color.decode("#b3b3b3"));
			}
			// Or just send plain text as a purple colour to better notify the user another user has replied to them
			else {
				appendToPane(textPane, "\n" + message.trim() + "   ", Color.decode("#e7c2ff"), false, 18, "Arial");  
			}
        }
		// This client is not being replied to or it is just a standard message being sent to the chat
		else {
			if (checkIfURLS(message.trim())) {
				sendMsgWithURLS(doc, message.trim(), Color.decode("#E2E2E2"));
			}
			else {
				appendToPane(textPane, "\n" + message.trim() + "   ", Color.decode("#E2E2E2"), false, 18, "Arial");  
			}
		}
        reply.setFont(new Font("Arial", Font.PLAIN, 10));
        reply.setForeground(Color.LIGHT_GRAY);
        reply.setCursor(new Cursor(Cursor.HAND_CURSOR));
		textPane.setCaretPosition(textPane.getDocument().getLength());
		textPane.insertComponent(reply);
		reply.setAlignmentY(0.90f);
		if (replying) {
			JLabel replied;
			// Will display the replied to message to the user who sent the reply
			if (repliedUsername == null && repliedMessage == null) {
				replied = new JLabel("Replied to " + globalReplyUsername + " \"" + globalReplyMsg + "\"");
			}
			// Will display the replied to message to all other users
			else {
				replied = new JLabel("Replied to " + repliedUsername + " \"" + repliedMessage + "\"");
			}
            replied.setFont(new Font("Arial", Font.BOLD, 12));
            replied.setForeground(Color.LIGHT_GRAY);
            insertNewLines(1, doc);
            textPane.setCaretPosition(textPane.getDocument().getLength());
			textPane.insertComponent(replied);
			textPane.setCaretPosition(textPane.getDocument().getLength());
			replied.setAlignmentY(0.55f);
        	globalReplyMsg = "";
        	replying = false;
        	replyLabel.setText("");
        	cancelLabel.setText("");
        } 
    }
    
    /*
     * Listen to messages from other users and the server in a separate thread to prevent
     * blocking
     * 
     * If a message is received handle it
     */
    public void listenForMessage() {
        new Thread(new Runnable() {
            @Override
            public void run() {            	
                String msgFromGroupChat;
                StyledDocument doc = textPane.getStyledDocument();
                while (sslSocket.isConnected()) {
                    try {                       
                        msgFromGroupChat = in.readLine();
                        System.out.println(msgFromGroupChat);
                        // Keeping track of current date for the timestamp on each message
                        date = new Date(System.currentTimeMillis());
                        if (msgFromGroupChat == null) {
                        	insertNewLines(2, doc);
                            appendToPane(textPane, "You have lost connection to the server!", Color.RED, true, 18, "Arial");
                            sendButton.setEnabled(false);
                            break;
                        } 
                        else {
                        	// If the message is not coming from one of the clients, i.e. coming from the server or something automated
                        	if (!msgFromGroupChat.startsWith("MESSAGEFROMCLIENT") && !msgFromGroupChat.startsWith("REPLYMESSAGEFROMCLIENT")) {
                        		// If it is one of the automated leaving/joining messages
                            	if (msgFromGroupChat.contains(" has entered the chat!") || msgFromGroupChat.contains(" has left the chat!")){
                            		insertNewLines(2, doc);
                                    textPane.setCaretPosition(textPane.getDocument().getLength());
                                    appendToPane(textPane, msgFromGroupChat, Color.decode("#F2F2F2"), true, 18, "Arial");
	                        		appendToPane(textPane, "   " + formatDate(formatter.format(date)), Color.LIGHT_GRAY, false, 12, "Helvetica");
	                        		
	                        		// Get the list of the names and parse it 
	                        		String readNames = in.readLine();
	                        		String parsedNames = readNames.replaceAll("\\[", "").replaceAll("\\]", "").replaceAll(",", "");
	                        		String[] split = parsedNames.split(" ");                    		
	                        		for (String name: split) {
	                        			if (!namesList.contains(name)) {
	                        				namesList.add(name);
	                        			}                     			
	                        		}
	                                Style style = textPane.addStyle("Color Style", null);
	                                StyleConstants.setForeground(style, Color.decode("#E2E2E2"));
	                                StyleConstants.setFontSize(style, 18);
	                        		Element root = doc.getDefaultRootElement();
	                        		Element content = root.getElement(2);
	                        		                        		
	                        		int start = content.getStartOffset();
	                        		int end = content.getEndOffset();
	                        		try {
	                        			doc.remove(start, end - start - 1);
	                        			// Update the currently connected list as users leave/join
	                        			doc.insertString(start, "Currently connected: " + readNames.replaceAll("\\[", "").replaceAll("\\]", ""), style);
	                        		}
	                        		catch (BadLocationException e) {
	                        			e.printStackTrace();
	                        		}
                            	}
                            	// Otherwise message must be coming from admin
                            	else {
                            		if (msgFromGroupChat.equals("KICK " + client.getUsername())) {
                                		kickClient(doc);
                                        break;
                                	}
                            		else if (msgFromGroupChat.contains("SAY "))	{
                            			sendMessageFromAdmin(msgFromGroupChat, doc);                          			
                                	}
                            		else if (msgFromGroupChat.contains("MUTE ")) {
                            			muteClient(msgFromGroupChat, doc);
                            			
                            		}
                            	}
                        	}
                        	// Else if the message is coming from a client
                    		else {
                    			// The client is replying to someone
                    			if (msgFromGroupChat.substring(0, 5).equals("REPLY")) {
                    				replying = true;
                    				String replace = msgFromGroupChat.replaceFirst("REPLYMESSAGEFROMCLIENT", "");
                                    String[] split = replace.split(" ", 2);
                                    String username = split[0];
                                    String msg = in.readLine();
                                    String[] splitReplyNameAndMessage = msg.split(" ", 2);
                                    String replyToUsername = splitReplyNameAndMessage[0];
                                    String replyToMsg = splitReplyNameAndMessage[1];                                  
                                    handleDisplayingMessages(username, doc, split[1], replyToUsername, replyToMsg);
                    			}
                    			// The client is not replying to someone
                    			else {
                    				replying = false;
                    				String replace = msgFromGroupChat.replaceFirst("MESSAGEFROMCLIENT", "");
                                    String[] split = replace.split(" ", 2);
                                    String username = split[0];
                                    handleDisplayingMessages(username, doc, split[1], null, null);
                    			}                                                        
                            }
                            // set caret position at the bottom so new messages are visible initially
                            textPane.setCaretPosition(textPane.getDocument().getLength());
                        }
                    }catch (IOException e) {
                        closeEverything(sslSocket, in , out);
                        insertNewLines(2, doc);
                        appendToPane(textPane, "You have lost connection to the server!", Color.RED, true, 18, "Arial");
                        sendButton.setEnabled(false);
                        break;
                    }
                }
            }
        }).start();
    }
    
    /*
     * Format the given date
     */
    public String formatDate(String date) {
        String dateUpper = date.toUpperCase();
        String dateFormatted = dateUpper.replace(".", "");

        return dateFormatted;
    }
    
    /*
     * Method used to send a message to other users and add the users sent message
     * to their own chat view
     */
    public void send() {
    	StyledDocument doc = textPane.getStyledDocument();
        date = new Date(System.currentTimeMillis());
        if (inputField.getText().trim().length() > 0) {       
        	if (inputField.getText().length() <= 1000) {
        		// If the reply label isn't empty, the user must be replying to someone
        		if (!replyLabel.getText().equals("")) {
        			replying = true;
        		}
        		sendMessageOverSocket(inputField.getText(), replying, globalReplyUsername, globalReplyMsg);
    			String username = client.getUsername();
                handleDisplayingMessages(username, doc, inputField.getText(), null, null);
                inputField.setText("");
        		if (System.currentTimeMillis() > lastTimeSent + 1000) {       			
        			lastTimeSent = System.currentTimeMillis();
        		}
                else {
                    insertNewLines(2, doc);
                    appendToPane(textPane, "Spam Filter: Unable to send messages for " + duration/1000 + " second(s)", Color.RED, true, 18, "Arial");
                	sendButton.setEnabled(false);
                	timer.start();
                	timer.setInitialDelay(duration);
                	timer.restart();
                	duration += 3000;
                	Runnable resetDurationLong = new Runnable() {
                    	public void run() {
                			duration = 1000;
                			longTimeOut = false;
                    	}
                    };
                    
                    Runnable resetDurationShort = new Runnable() {
                    	public void run() {
                    		if (shortTimeOut && !longTimeOut) {
                    			duration = 1000;
                        		shortTimeOut = false;
                    		}
                    	}
                    };
                    // Reset the timeout duration after given time
                    if (duration > 10000 && !longTimeOut) {
                    	ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
                    	executor.schedule(resetDurationLong, 900, TimeUnit.SECONDS);
                    	longTimeOut = true;
                    	shortTimeOut = false;
                    }
                    else if (duration < 10000 & !shortTimeOut){
                    	ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
                    	executor.schedule(resetDurationShort, 120, TimeUnit.SECONDS);
                    	shortTimeOut = true;
                    }
                }
        	}
        	else {
        		JOptionPane.showMessageDialog(null, "Exceeded character cap. Max characters per message is 1000.",
                        "Warning", JOptionPane.WARNING_MESSAGE);
        	}
    	}         
    }
       
    private class WrapEditorKit extends StyledEditorKit {
        ViewFactory defaultFactory = new WrapColumnFactory();
        public ViewFactory getViewFactory() {
            return defaultFactory;
        }
    }

    private class WrapColumnFactory implements ViewFactory {
        public View create(Element elem) {
            String kind = elem.getName();
            if (kind != null) {
                if (kind.equals(AbstractDocument.ContentElementName)) {
                    return new WrapLabelView(elem);
                } else if (kind.equals(AbstractDocument.ParagraphElementName)) {
                    return new ParagraphView(elem);
                } else if (kind.equals(AbstractDocument.SectionElementName)) {
                    return new BoxView(elem, View.Y_AXIS);
                } else if (kind.equals(StyleConstants.ComponentElementName)) {
                    return new ComponentView(elem);
                } else if (kind.equals(StyleConstants.IconElementName)) {
                    return new IconView(elem);
                }
            }

            // default to text display
            return new LabelView(elem);
        }
    }

    private class WrapLabelView extends LabelView {
        public WrapLabelView(Element elem) {
            super(elem);
        }

        public float getMinimumSpan(int axis) {
            switch (axis) {
                case View.X_AXIS:
                    return 0;
                case View.Y_AXIS:
                    return super.getMinimumSpan(axis);
                default:
                    throw new IllegalArgumentException("Invalid axis: " + axis);
            }
        }
    }
    
    /*
     * If the user clicks a hyperlink this class will get the user's default browser and navigate to the 
     * clicked link on the web
     */
    private class URLLinkAction extends AbstractAction {
    	private String url;
    	
    	private URLLinkAction(String url) {
    		this.url = url;
    	}
    	
    	public void execute() {
    		try {
    		    Desktop.getDesktop().browse(new URL(url).toURI());
    		} catch (Exception e) {
    			e.printStackTrace();
    		}
    	}

		@Override
		public void actionPerformed(ActionEvent e) {
			execute();
		}
    }
}
