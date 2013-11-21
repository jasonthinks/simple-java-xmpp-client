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
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
 
public class XmppClient implements MessageListener, ChatManagerListener {
 	static Logger logger = LoggerFactory.getLogger(XmppClient.class);
	public static final String DOMAIN_NAME;
	public static final String HOST;
	public static final int PORT;
	public static final Boolean DEBUG;
	ChatMap chatsPool = new ChatMap();
    XMPPConnection connection;
    int counter = 0;
    MessagePool msgPool = null;
	private final String username;

	static {
    	Properties props = new Properties();
			try {
				props.load(new FileReader(new File("xmpp.properties")));
			} catch (Exception e) {
				logger.warn("Error occurred when loading file [xmpp.properties]: {}", e.getMessage());
			}
			DOMAIN_NAME = props.getProperty("xmpp.domain", "localhost");
			HOST = props.getProperty("xmpp.host", "localhost");
			PORT = Integer.parseInt(props.getProperty("xmpp.port", "5222"));
			DEBUG = Boolean.parseBoolean(props.getProperty("xmpp.debug", "false"));
			logger.debug("xmpp.domain = [{}], xmpp.host = [{}], xmpp.port = [{}], xmpp.debug = [{}]", DOMAIN_NAME,
					HOST, PORT, DEBUG);
    } 
    
    public XmppClient(String username, String password, MessagePool msgPool) throws Exception {
    	this.username = username;
    	XMPPConnection.DEBUG_ENABLED = DEBUG;
    	login(username, password);
    	this.msgPool = msgPool; 
    }
    
    private void login(String username, String password) throws Exception {
    	Roster.setDefaultSubscriptionMode(Roster.SubscriptionMode.accept_all);
        ConnectionConfiguration config = new ConnectionConfiguration(HOST, PORT, DOMAIN_NAME);
        config.setSASLAuthenticationEnabled(false);
        SASLAuthentication.supportSASLMechanism("PLAIN", 0);
		config.setDebuggerEnabled(false);
		config.setNotMatchingDomainCheckEnabled(false);
		config.setSecurityMode(SecurityMode.disabled);
		config.setReconnectionAllowed(true);
		config.setSendPresence(false);
		
        connection = new XMPPConnection(config);
        connection.connect();
        
        try {
        	
        	connection.login(username, password);
        	Presence presence = new Presence(Presence.Type.available);
        	connection.sendPacket(presence);
        	logger.debug("User [{}/{}] logged in and got line.", username, password);
        }
        catch(Exception e) {
        	logger.warn("User [{}/{}] login failed", username, password);
        	if(connection.getAccountManager().supportsAccountCreation()) {
        		try {
					connection.getAccountManager().createAccount(username, password);
					logger.info("User [{}/{}] created successfully", username, password);
					connection.login(username, password);
					Presence presence = new Presence(Presence.Type.available);
		        	connection.sendPacket(presence);
				} catch (Exception e1) {
					logger.warn("Auto-registration is supported but exception occurred", e);
					throw e1;
				}
        	}
        	else {
        		logger.warn("Auto-registration is not supported: {}", e.getMessage());
        		throw e;
        	}
        }
        connection.getChatManager().addChatListener(this);
    }

    public void sendMessage(String message, String to) throws XMPPException {
    	logger.debug("User [{}] is sending message [{}] to user [{}]", this.getUsername(), message, to);
        Chat chat = null;
        to = to + "@" + DOMAIN_NAME;
        confirmInRoster(to);
        if(chatsPool.containsKey(to)){
        	chat = chatsPool.get(to);
        }
        else {
        	chat = connection.getChatManager().createChat(to, this);
        	chatsPool.put(to, chat);
        }
        chat.sendMessage(message);
    }
    
    private void confirmInRoster(String jid) {
    	Roster roster = connection.getRoster();
        Collection<RosterEntry> entries = roster.getEntries();
        for (RosterEntry r : entries) {
        	if(r.getUser().startsWith(jid)){
        		return;
        	}
        }
        try {
			roster.createEntry(jid, jid, null);
		} catch (XMPPException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
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
        	logger.debug("User [{}] received a message [{}] from user [{}]", message.getTo(), message.getBody(), message.getFrom());
        	msgPool.addMessage(chat, message);
        	String jid = chat.getParticipant().substring(0, chat.getParticipant().indexOf("/"));
            if(!chatsPool.containsKey(jid)) {
            	chatsPool.put(jid, chat);
            }
        }
    }
    
    @Override
	public void chatCreated(Chat chat, boolean createdLocally) {
		if(!createdLocally) {
			logger.debug("User [{}] received a chat from user [{}]", this.getUsername(), chat.getParticipant());
			chat.addMessageListener(this);
//			String jid = chat.getParticipant().substring(0, chat.getParticipant().indexOf("/"));
//			chatsPool.put(jid, chat);
		}
	}
    
    public static void main(String args[]) throws XMPPException, IOException {
    	String username = System.getProperty("username");
		String password = System.getProperty("password");
        XmppClient c = null;
		try {
			c = new XmppClient(username, password, new MessagePool(){
				@Override
				public void addMessage(Chat from, Message message) {
					System.out.println("[" + message.getFrom().substring(0, message.getFrom().indexOf("@")) + "] said: " + message.getBody());
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String msg;

        c.displayBuddyList();
        System.out.println("=========================================");
        System.out.println("Who do you want to talk to? - Type contact's username:");
        String talkTo = br.readLine();
        System.out.println("-----");
        System.out.println("All messages will be sent to " + talkTo);
        System.out.println("Enter your message in the console:");
        System.out.println("-----\n");
 
        while (!(msg = br.readLine()).equals("bye")) {
        	if(msg != null && msg.startsWith("to:")) {
        		talkTo = msg.substring(3);
        		System.out.println("=====================================\nAll messages will be sent to " + talkTo);
        	}
        	else if(msg != null && msg.startsWith("chats")) {
        		for(String s: c.chatsPool.keySet()) {
        			System.out.println("\t" + s);
        		}
        	}
        	else if(msg != null && msg.startsWith("help")) {
       			System.out.println("Available commands: \n\tchats\n\tbuddies\n\tto:somebody\n\thelp\n\tbye");
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
    
    public String getUsername() {
		return username;
	}
    
    public void increaseCounter() {
		this.counter++;
	}
	public void decreaseCounter() {
		this.counter--;
	}
	public int getCounter(){
		return this.counter;
	}
	public void deleteAccount(){
		try {
			connection.getAccountManager().deleteAccount();
		} catch (XMPPException e) {
			logger.warn("Failed to delete user account [{}].", this.getUsername());
		}
	}
}

class ChatMap extends HashMap<String, Chat>{
	Logger logger = LoggerFactory.getLogger(ChatMap.class);
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
		logger.debug("Chat with [{}] added to chats' pool", key);
		return super.put(key, value);
	}
	
}
