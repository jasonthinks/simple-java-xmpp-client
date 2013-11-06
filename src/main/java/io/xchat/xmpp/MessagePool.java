package io.xchat.xmpp;

import org.jivesoftware.smack.packet.Message;

public interface MessagePool {
	public void addMessage(String from, Message message);

}
