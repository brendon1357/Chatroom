import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

/*
 * Server to handle client connections and listen for input from admin
 */
public class Server {
    private SSLServerSocket serverSocket;
    private static ArrayList<String> names;
    private static ArrayList<ClientHandler> clientHandlers = new ArrayList<>();
    private static Logger logger;

    public Server(SSLServerSocket serverSocket) {
    	this.serverSocket = serverSocket;
    	names = new ArrayList<>();
    }

    /*
     * Start the server and keep making new ClientHandler threads as needed
     */
    private void startServer() {
     	try {
     		System.out.println("Server started successfully...\n");
     		System.out.println("Type EXIT to close the server\n");
            while (!serverSocket.isClosed()) {
            	SSLSocket sslSocket = (SSLSocket)serverSocket.accept();
            	ClientHandler clientHandler = new ClientHandler(sslSocket);         	
            	Thread thread = new Thread(clientHandler);
            	thread.start();           	
            }                 
        } catch (IOException e) {
        	logger.severe(e.getMessage());
            closeServerSocket();
        }
    }
    
    /*
     * Listen for input on cmd line in separate thread
     */
    private static void listenForInput() {
    	new Thread(new Runnable() {
    		@Override
    		public void run() {
    			Scanner sc = new Scanner(System.in);
    			while (sc.hasNextLine()) {
    				String msg = sc.nextLine();
    				if (msg.equals("EXIT")) {
    					System.exit(0);
    					break;
    				}
    				else {
    					for (ClientHandler clientHandler : clientHandlers) {
        					clientHandler.out.println(msg);
        					clientHandler.out.flush();
        				}
    				}   				
				}
			    sc.close();
			}
		}).start();
    }
    
    /*
     * Close the server socket
     */
    private void closeServerSocket() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
        	logger.severe(e.getMessage());
        }
    }
    
    /*
     * Program driver
     */
    public static void main(String[] args) throws IOException {
    	System.setProperty("javax.net.ssl.keyStore","PathToKeyStore");
        System.setProperty("javax.net.ssl.keyStorePassword","KeyStorePassword");
        File file = new File("Logfile.log");
 		file.createNewFile();
 		logger = Logger.getLogger("Server");  
 	    FileHandler fh;  
 	    try {  
 	        fh = new FileHandler("Logfile.log");  
 	        logger.addHandler(fh);
 	        SimpleFormatter formatter = new SimpleFormatter();  
 	        fh.setFormatter(formatter);  
 	    } catch (SecurityException e) {  
 	        e.printStackTrace();  
 	    } catch (IOException e) {  
 	        e.printStackTrace();  
 	    } 
        listenForInput();
    	SSLServerSocketFactory sslServerSocketfactory = (SSLServerSocketFactory)SSLServerSocketFactory.getDefault();
        SSLServerSocket sslServerSocket = (SSLServerSocket)sslServerSocketfactory.createServerSocket(33333);
        Server server = new Server(sslServerSocket);
        server.startServer();                 
    }
    
    /*
     * ClientHandler class handles everything related to connected clients
     */
    private class ClientHandler implements Runnable {
        private SSLSocket sslSocket;
        private BufferedReader in;
        private PrintWriter out;
        private String clientUsername;
        
        public ClientHandler(SSLSocket sslSocket) {
            try {
                this.sslSocket = sslSocket;
                in = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));
                out = new PrintWriter(new OutputStreamWriter(sslSocket.getOutputStream()));
            }
            catch (IOException e) {
            	logger.severe(e.getMessage());
                closeEverything(sslSocket, in, out);
            }           
        }
        
        /*
         * Keep listening for messages from all clients in a separate thread and broadcast those messages
         * to every client
         */
        @Override
        public void run() {
        	boolean disconnected = false;
            while (!disconnected) {
            	try {
    				if (validate()) {
    					while (!disconnected) {
    						String messageFromClient = in.readLine();
    						if (messageFromClient == null) {
    							disconnected = true;
    							break;
    						}
    						if (!(messageFromClient.equals(clientUsername))) {
    							broadcastMessage(messageFromClient);
    						}
    					}
    					if (disconnected) {
    						closeEverything(sslSocket, in, out);
    					}
    				}
    			} catch (IOException e) {
    				logger.severe(e.getMessage());
    				if (!disconnected) {
    					disconnected = true;
    					closeEverything(sslSocket, in, out);
    					break;
    				}
    			}      	
            }
        }
        
        /*
         * Reads input from client trying to connect, validates that input, and sends a message
         * back to client side indicating if valid input or not
         */
        private boolean validate() {  	
        	while (true) {
				try {
					clientUsername = in.readLine();
				} catch (IOException e) {
					return false;
				}
				if (clientUsername == null) {
					closeEverything(sslSocket, in, out);
        			return false;
				}
				String password = null;
				try {
					password = in.readLine();
				} catch (IOException e) {
					return false;
				}
				if (password == null) {
					closeEverything(sslSocket, in, out);	
        			return false;
				}
				if (clientUsername.equals("") || password.equals("#PASSWORD: ")) {
					logger.info("A client has tried to connect with empty input. Failed connection.\n");
    				out.println("Failed Connection Input Empty");
    				out.flush();
		    		return false;
		    	}
				StringBuilder str = new StringBuilder();
				str.append("Client trying to connect: " + clientUsername + ", " + sslSocket.getInetAddress().getHostAddress().toString().replace("/", ""));
				str.append("\n");
				str.append("      Received " + password + " from " + clientUsername);
				
        		String passwordFormatted = password.replaceFirst("#PASSWORD: ", "");
        		if (passwordFormatted.equals("1234") && !(clientUsername.contains(" ") && !(clientUsername.equals("")))) {
        			if (clientUsername.length() > 25) {
        				str.append("\n");
        				str.append("      Client " + clientUsername + " has failed to connected. Username too long.\n");
        				logger.info(str.toString());
        				out.println("Failed Connection Username Too Long");
        				out.flush();
        				return false;
        			}
    				synchronized (names) {
            			if (!names.contains(clientUsername)) {
            				synchronized (clientHandlers) {
            					clientHandlers.add(this);
            				}           				
            				names.add(clientUsername);
            				str.append("\n");
            				str.append("      Client " + clientUsername + " has connected successfully");
            				str.append("\n");
            				str.append("      " + names + "\n");
            				logger.info(str.toString());
                			broadcastMessage(clientUsername + " has entered the chat!");
                			out.println("Successful Connection");
                			out.flush();
                			out.println(names);
                			out.flush();
                			return true;
            			}
            			else {
            				str.append("\n");
            				str.append("      Client " + clientUsername + " has failed to connected. Username already exists.\n");
            				logger.info(str.toString());
                			clientUsername = null;      			
                			out.println("Failed Connection Username Exists");
                			out.flush();
                			return false;
            			}
            		}           			 			
        		}
        		else {
        			str.append("\n");
    				str.append("      Client " + clientUsername + " has failed to connected. Invalid input.\n");
    				logger.info(str.toString());
        			clientUsername = null;      			
        			out.println("Failed Connection");
        			out.flush();
        			return false;
        		}
    		}
    	}
        
        /*
         * Broadcast message to all connected clients
         */
        private void broadcastMessage(String messageToSend) {
            for (ClientHandler clientHandler : clientHandlers) {
    			if (!clientHandler.clientUsername.equals(clientUsername)) {
    				if (messageToSend.contains("MESSAGEFROMCLIENT") || (!messageToSend.contains(" has entered the chat!") && !messageToSend.contains(" has left the chat!"))) {
    					clientHandler.out.println(messageToSend);
        			    clientHandler.out.flush();
    				}
    				else {
						clientHandler.out.println(messageToSend);
        			    clientHandler.out.flush();
    					clientHandler.out.println(names);
    					clientHandler.out.flush();
    				}
    			}
            }
        }
        
        /*
         * Remove ClientHandler if client is disconnected from server
         */
        private void removeClientHandler() {
        	synchronized (clientHandlers) {
        		 clientHandlers.remove(this);
        	}       
            synchronized (names) {
            	names.remove(clientUsername);
            }
            if (!(clientUsername == null) && !(clientUsername.equals(""))) {
            	StringBuilder str = new StringBuilder();
            	str.append("Client " + clientUsername + " has disconnected");
            	str.append("\n");
            	str.append("      " + names + "\n");
            	logger.info(str.toString());
            	broadcastMessage(clientUsername + " has left the chat!"); 
            }      	       
        }
        
        /*
         * Close the readers/writers and socket
         */
        private void closeEverything(SSLSocket socket, BufferedReader in, PrintWriter out) {
            removeClientHandler();
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
            	logger.severe(e.getMessage());
            }
        }
    }
}
