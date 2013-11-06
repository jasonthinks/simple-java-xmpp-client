package io.xchat.xmpp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Properties;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
 
public class XmppClient implements MessageListener, ChatManagerListener {
 	
	public static final String DOMAIN_NAME;
	public static final String HOST;
	public static final int PORT;
	ChatMap chatsPool = new ChatMap();
	
    XMPPConnection connection;
 
    MessagePool msgPool = null;
    
    static {
    	Properties props = new Properties();
			try {
				props.load(new FileReader(new File("xmpp.properties")));
			} catch (Exception e) {
				e.printStackTrace();
			}
			DOMAIN_NAME = props.getProperty("xmpp.domain", "localhost");
			HOST = props.getProperty("xmpp.host", "localhost");
			PORT = Integer.parseInt(props.getProperty("xmpp.port", "5222"));
    } 
    
    public XmppClient(String username, String password, MessagePool msgPool) throws XMPPException {
    	XMPPConnection.DEBUG_ENABLED = true;
    	login(username, password);
    	this.msgPool = msgPool; 
    }
    
    private void login(String userName, String password) throws XMPPException {
        ConnectionConfiguration config = new ConnectionConfiguration(HOST, PORT, DOMAIN_NAME);
        connection = new XMPPConnection(config);
        connection.connect();
        SASLAuthentication.supportSASLMechanism("PLAIN", 0);
        try {
        	connection.login(userName, password);
        	
        }
        catch(Exception e) {
        	if(connection.getAccountManager().supportsAccountCreation()) {
        		try {
					connection.getAccountManager().createAccount(userName, password);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
        	}
        }
        connection.getChatManager().addChatListener(this);
    }

    public void sendMessage(String message, String to) throws XMPPException {
        Chat chat = null;
        if(chatsPool.containsKey(to)){
        	System.out.println("use existing chat with " + to);
        	chat = chatsPool.get(to);
        }
        else {
        	System.out.println("create new chat with " + to);
        	chat = connection.getChatManager().createChat(to, this);
        	chatsPool.put(to, chat);
        }
        chat.sendMessage(message);
    }
    
    public void displayBuddyList() {
        Roster roster = connection.getRoster();
        Collection<RosterEntry> entries = roster.getEntries();
        System.out.println(entries.size() + " buddy(ies):");
        for (RosterEntry r : entries) {
            System.out.println(r.getUser());
        }
    }
 
    public void disconnect() {
        connection.disconnect();
    }
 
    @Override
	public void processMessage(Chat chat, Message message) {
        if (message.getType() == Message.Type.chat) {
        	msgPool.addMessage(chat.getParticipant(), message);
//            System.out.println(chat.getParticipant() + " says: " + message.getBody());
//            try {
//                chat.sendMessage(message.getBody() + " echo");
//            } catch (XMPPException ex) {
//                Logger.getLogger(XmppClient.class.getName()).log(Level.SEVERE, null, ex);
//            }
            if(!chatsPool.containsKey(chat.getParticipant())) {
            	
            	chatsPool.put(chat.getParticipant(), chat);
            }
        }
    }
    @Override
	public void chatCreated(Chat chat, boolean createdLocally) {
		if(!createdLocally) {
			System.out.println("chat received from [" + chat.getParticipant() + "]");
			chat.addMessageListener(this);
			chatsPool.put(chat.getParticipant(), chat);
		}
	}
    
    public static void main(String args[]) throws XMPPException, IOException {
    	String username = System.getProperty("username");
		String password = System.getProperty("password");
        XmppClient c = new XmppClient(username, password, new MessagePool(){
			@Override
			public void addMessage(String from, Message message) {
				System.out.println("[" + from + "] said: " + message.getBody());
			}
        });
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String msg;

        c.displayBuddyList();
        System.out.println("=========================================");
        System.out.println("Who do you want to talk to? - Type contact's username:");
        String talkTo = br.readLine();
        talkTo = talkTo + "@" + DOMAIN_NAME;
        System.out.println("-----");
        System.out.println("All messages will be sent to " + talkTo);
        System.out.println("Enter your message in the console:");
        System.out.println("-----\n");
 
        while (!(msg = br.readLine()).equals("bye")) {
        	if(msg != null && msg.startsWith("to:")) {
        		talkTo = msg.substring(3) + "@" + DOMAIN_NAME;
        		System.out.println("=====================================\nAll messages will be sent to " + talkTo);
        	}
        	else if(msg != null && msg.startsWith("chats")) {
        		for(String s: c.chatsPool.keySet()) {
        			System.out.println("\t" + s);
        		}
        	}
        	else if(msg != null && msg.startsWith("help")) {
       			System.out.println("Available commands: \n\tchats\n\tbuddies\nto:somebody\n\thelp\n\tbye");
        	}
        	else if(msg != null && msg.startsWith("buddies")) {
        		c.displayBuddyList();
        	}
        	else {
        		c.sendMessage(msg, talkTo);
        	}
        }
 
        c.disconnect();
        System.exit(0);
    }

	
}

class ChatMap extends HashMap<String, Chat>{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public Chat put(String key, Chat value) {
		if(key.indexOf("/") > 0) {
			key = key.substring(0, key.indexOf("/"));
		}
		if(key.indexOf("@") < 0) {
			key = key + "@" + XmppClient.DOMAIN_NAME;
		}
		return super.put(key, value);
	}
	
}