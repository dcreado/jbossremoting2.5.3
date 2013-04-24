package org.jboss.remoting.samples.chat.server;

/**
 * <p>Title: Chat4</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 * @author not attributable
 * @version 1.0
 */

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

import org.jboss.remoting.samples.chat.client.ChatInfo;
import org.jboss.remoting.samples.chat.client.ChatMember;
import org.jboss.remoting.samples.chat.exceptions.NameInUseException;
import org.jboss.remoting.samples.chat.utility.ReadWriteArrayList;

public class ExtendedChatInfo implements Serializable
{
  private static final long serialVersionUID = 3;

  private ChatInfo chatInfo;
  private ReadWriteArrayList messages;
  private Collection members;
  transient private Hashtable threadMap;
  transient private Collection chatReceivers;

  public ExtendedChatInfo(ChatInfo chatInfo)
  {
    this.chatInfo = chatInfo;
    messages = new ReadWriteArrayList();
    members =  Collections.synchronizedCollection(new HashSet());
    threadMap =  new Hashtable();
    chatReceivers = Collections.synchronizedCollection(new HashSet());
  }

  private void readObject(java.io.ObjectInputStream in)
     throws IOException, ClassNotFoundException
 {
   in.defaultReadObject();
   threadMap =  new Hashtable();
   chatReceivers = Collections.synchronizedCollection(new HashSet());
 }

  public ChatInfo getChatInfo()
  {return chatInfo;}

  public Collection getMembers()
  {return members;}

  public ReadWriteArrayList getMessages()
  {return messages;}

  public Collection getChatReceivers()
  {return chatReceivers;}

  public CallbackThread getChatReceiverThread(ChatMember member)
  { return (CallbackThread) threadMap.get(member.get_name()); }

  public void addMember(ChatMember member)
      throws NameInUseException
  {
    String name = member.get_name();

    if (members.contains(name))
        throw new NameInUseException();

    members.add(name);
  }

  public void removeMember(ChatMember member)
  {
    String name = member.get_name();
    threadMap.remove(name);
    members.remove(name);
  }


  public void addMessages(ArrayList messages)
  {
    Iterator it = messages.iterator();
    while (it.hasNext())
    {
      this.messages.add(it.next());
    }
  }

  public void addMessage(String message)
  {
    messages.add(message);
  }


}
