package org.jboss.remoting.samples.chat.server;

/**
 * <p>Title: Chat4</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 * @author not attributable
 * @version 1.0
 */

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;

import org.jboss.logging.Logger;
import org.jboss.remoting.samples.chat.client.ChatInfo;
import org.jboss.remoting.samples.chat.exceptions.ShuttingDownException;
import org.jboss.remoting.samples.chat.utility.ShutDownGate;

public class ChatStore implements Serializable
{
  private static final long serialVersionUID = 1;
  protected static final Logger log = Logger.getLogger(ChatStore.class);

  private Hashtable extendedChatInfoMap;
  private ShutDownGate shutDownGate;

  public ChatStore()
  {
    extendedChatInfoMap = new Hashtable();
    shutDownGate = new ShutDownGate();
    
//    try
//    {
//       databaseManager = new DatabaseManager();
//       
//       System.out.println("ChatStore(): created DatabaseManager");
//       
//       if (databaseManager.chatDBExists())
//       {
//          restoreExtendedChatInfoMap();
//          System.out.println("ChatStore(): restored chat store");
//       }
//       else
//       {
//          databaseManager.createChatDB();
//          System.out.println("ChatStore(): created new data base");
//       }
//    }
//    catch (DatabaseException e)
//    {
//       e.printStackTrace();
//    }
  }


  public ShutDownGate getShutDownGate()
  { return shutDownGate; }

  
  public void addChat(ExtendedChatInfo eci) throws ShuttingDownException
  {
    shutDownGate.enter();
    String key = eci.getChatInfo().get_key();
    extendedChatInfoMap.put(key, eci);
    log.info("adding chat " + key);
    shutDownGate.leave();
  }

  
/* fail-fast iterator*/
  ArrayList listChats() throws ShuttingDownException
  {
    shutDownGate.check();
    Collection c = extendedChatInfoMap.values();
    ArrayList chatArrayList = new ArrayList();

    Iterator it = c.iterator();
    while (it.hasNext())
    {
      ExtendedChatInfo eci = (ExtendedChatInfo) it.next();
      ChatInfo ci = eci.getChatInfo();
      ci.set_currentMembers(eci.getMembers().size());
      ci.set_size(eci.getMessages().size());
      chatArrayList.add(ci);
      log.debug("eci.getMembers(): " + eci.getMembers());
    }

    return chatArrayList;
  }

  
  public ExtendedChatInfo getChat(String key) throws ShuttingDownException
  {
    shutDownGate.check();
    ExtendedChatInfo eci = (ExtendedChatInfo) extendedChatInfoMap.get(key);
    return eci;
  }

  
  public Collection getChatKeySet()
  { return extendedChatInfoMap.keySet(); }


//  private void restoreExtendedChatInfoMap()
//  {
//    ArrayList chats = null;
//
//    try
//   {
//      chats = databaseManager.getChats();
//   }
//   catch (DatabaseException e)
//   {
//      e.printStackTrace();
//   }
//   Iterator it = chats.iterator();
//
//   while (it.hasNext())
//   {
//     ChatInfo chatInfo = (ChatInfo) it.next();
//     ExtendedChatInfo eci = new ExtendedChatInfo(chatInfo);
//
////        ArrayList members = databaseManager.getChatMembers(chatInfo.get_key());
////        Iterator mit = members.iterator();
////
////        while (mit.hasNext())
////        {
//     //          eci.addMember((ChatMember) it.next(), null);
//     //        }
//     
//     ArrayList messages;
//     try
//     {
//        messages = databaseManager.getChatMessages(chatInfo.get_key());
//        eci.addMessages(messages);
//     }
//     catch (DatabaseException e1)
//     {
//        e1.printStackTrace();
//     }
//     
//     
//     
//     try {
//        addChat(eci);
//     } catch (ShuttingDownException sde) {} // ignore: we're just starting up
//   }
//  }
  
}

