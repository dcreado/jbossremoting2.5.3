package org.jboss.remoting.samples.chat.client;

import org.jboss.remoting.samples.chat.exceptions.RemoteConnectionException;

public interface ChatReceiver
{
	void setKey(Integer key) throws RemoteConnectionException;
    void send(ChatMessage mesg) throws RemoteConnectionException;
    void sendMultiple(java.util.ArrayList messages) throws RemoteConnectionException;
    void shuttingDown() throws RemoteConnectionException;
}
