package ltg.commons.examples;

import ltg.commons.SimpleXMPPClient;

/**
 * This example demonstrates the use of SimpleXMPPClient class.
 * The class is used to synchronously wait for packets and then process them,
 * a typical scenario when designing simple agents.
 * 
 * @author tebemis
 *
 */
public class SynchronousXMPPClient {

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
		
		// ... so let's go ahead and wait for a message to arrive...
		while (!Thread.currentThread().isInterrupted()) {
			// ... and process it ...
			System.out.println(sc.nextMessage());
		}
		// ... and finally disconnect.
		sc.disconnect();
	}

}
