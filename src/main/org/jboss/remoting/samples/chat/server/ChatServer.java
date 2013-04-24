package org.jboss.remoting.samples.chat.server;

import org.jboss.remoting.samples.chat.client.ChatInfo;
import org.jboss.remoting.samples.chat.client.ChatMember;
import org.jboss.remoting.samples.chat.client.ChatMessage;
import org.jboss.remoting.samples.chat.exceptions.RemoteConnectionException;
import org.jboss.remoting.samples.chat.exceptions.ShuttingDownException;

public interface ChatServer
{
    void send(ChatMessage mesg) throws RemoteConnectionException, ShuttingDownException;
    ChatInfo getChatInfo() throws RemoteConnectionException, ShuttingDownException;
    java.util.ArrayList getBackChat() throws RemoteConnectionException, ShuttingDownException;
    void leave(ChatMember member) throws RemoteConnectionException, ShuttingDownException;
}
