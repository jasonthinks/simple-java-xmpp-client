package io.xchat.xmpp;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.packet.Message;

public interface MessagePool {
	public void addMessage(Chat from, Message message);

}
