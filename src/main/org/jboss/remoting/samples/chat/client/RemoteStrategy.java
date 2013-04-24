package org.jboss.remoting.samples.chat.client;

import java.util.ArrayList;

import org.jboss.remoting.samples.chat.exceptions.NameInUseException;
import org.jboss.remoting.samples.chat.server.ChatServer;

// ****************************************************************
public interface RemoteStrategy
{
  ArrayList list() throws org.jboss.remoting.samples.chat.exceptions.RemoteConnectionException, org.jboss.remoting.samples.chat.exceptions.ShuttingDownException;

  ChatServer createChat(String description,
                              ChatMember owner,
                              TalkFrame talkFrame,
                              org.jboss.remoting.samples.chat.utility.ReadWriteArrayList outgoingLines)
      throws NameInUseException, org.jboss.remoting.samples.chat.exceptions.RemoteConnectionException, org.jboss.remoting.samples.chat.exceptions.ShuttingDownException;

  ChatServer join(String key,
                        ChatMember newMember,
                        TalkFrame talkFrame,
                        org.jboss.remoting.samples.chat.utility.ReadWriteArrayList outgoingLines)
      throws NameInUseException, org.jboss.remoting.samples.chat.exceptions.RemoteConnectionException, org.jboss.remoting.samples.chat.exceptions.ShuttingDownException;

  void setShuttingDown();
}