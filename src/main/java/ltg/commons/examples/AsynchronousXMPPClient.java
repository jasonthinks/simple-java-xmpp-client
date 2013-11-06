package ltg.commons.examples;

import org.jivesoftware.smack.packet.Message;

import ltg.commons.MessageListener;
import ltg.commons.SimpleXMPPClient;

/**
 * This example demonstrates the use of SimpleXMPPClient class.
 * The class is used to asynchronously process packets.
 * This is useful whenever the main thread is already busy doing 
 * stuff and we want to interrupt.
 * 
 * @author tebemis
 *
 */
public class AsynchronousXMPPClient {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String username = System.getProperty("username");
		String password = System.getProperty("password");
		String chatRoom = System.getProperty("chatRoom");
		SimpleXMPPClient sc = (chatRoom==null)?new SimpleXMPPClient(username, password, chatRoom):new SimpleXMPPClient(username, password);
		
		
		// We are now connected and in the group chat room. If we don't do something
		// the main will terminate... 
		
		// ... so let's go ahead and make ourself busy... but before we do that... 
		 
		// ... let's register the packet handler with our XMPP client...
		sc.registerEventListener(new MessageListener() {
			
			@Override
			public void processMessage(Message m) {
				System.out.println(m.getBody());
			}
		});
		
		// ... and now we can make ourselves busy
		while(true) {
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				sc.disconnect();
			}
		}
		
		// NOTE: unless the main thread is suspended we will never get to this point 
	}

}
