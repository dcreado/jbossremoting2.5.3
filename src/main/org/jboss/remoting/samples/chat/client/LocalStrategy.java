package org.jboss.remoting.samples.chat.client;

import java.awt.Component;
import java.util.ArrayList;

import javax.swing.SwingUtilities;

import org.jboss.logging.Logger;
import org.jboss.remoting.samples.chat.exceptions.ConnectionException;
import org.jboss.remoting.samples.chat.exceptions.CreateConnectionException;
import org.jboss.remoting.samples.chat.exceptions.JoinConnectionException;
import org.jboss.remoting.samples.chat.exceptions.ListConnectionException;
import org.jboss.remoting.samples.chat.exceptions.NameInUseException;
import org.jboss.remoting.samples.chat.exceptions.RemoteConnectionException;
import org.jboss.remoting.samples.chat.exceptions.ShuttingDownException;
import org.jboss.remoting.samples.chat.exceptions.TalkConnectionException;
import org.jboss.remoting.samples.chat.server.ChatServer;
import org.jboss.remoting.samples.chat.utility.ReadWriteArrayList;

class Wrapper
{
   private Object obj;

   public Wrapper()
   {
   }

   public Wrapper(Object o)
   {
      obj = o;
   }

   public void set(Object o)
   {
      obj = o;
   }

   public Object get()
   {
      return obj;
   }
}

// ****************************************************************

public class LocalStrategy
implements ConnectionStrategy, CreateConnectionStrategy, InfoConnectionStrategy, JoinConnectionStrategy, ListConnectionStrategy
{
   protected static final Logger log = Logger.getLogger(LocalStrategy.class);
   
   private RemoteStrategy remoteStrategy;
   private CloseableFrame parent;
   private CreateFrame createFrame;
   private InfoFrame infoFrame;
   private JoinFrame joinFrame;
   private ListFrame listFrame;
   private TalkFrame talkFrame;
   private Thread readThread;
   private boolean isChatting;
   private ShutDownDialog shuttingDownDialog = new ShutDownDialog();
   
   
   public LocalStrategy(CloseableFrame parent, RemoteStrategy remoteStrategy)
   {
      this.parent = parent;
      this.remoteStrategy = remoteStrategy;
   }

   
   // ****************************************************************
   // ConnectionStrategy
   public void list()
   {
      final Wrapper chatRoomDescriptions = new Wrapper();

      Thread t = new Thread()
      {
         public void run()
         {
            try
            {
               chatRoomDescriptions.set(remoteStrategy.list());
            }
            catch (ShuttingDownException e)
            {
               SwingUtilities.invokeLater(shuttingDownDialogRunnable);
            }
            catch (RemoteConnectionException e)
            {
               log.error(e);
            }

            Runnable r = new Runnable()
            {
               public void run()
               {
                  new ListFrame(LocalStrategy.this, (ArrayList) chatRoomDescriptions.get(), parent).show();
               }
            };

            SwingUtilities.invokeLater(r);
         }
      };

      t.start();
   }

   
   public void create() throws ConnectionException
   {
      createFrame = new CreateFrame(this, parent);
      createFrame.show();
   }

   
   // ****************************************************************
   // CreateConnectionStrategy
   public void createChat(final String description, final ChatMember owner) throws CreateConnectionException
   {
      Thread t = new Thread()
      {
         TalkFrame talkFrame = new TalkFrame(description, owner.get_name(), parent);
         ReadWriteArrayList outgoingLines = new ReadWriteArrayList();
         ChatServer chatServer = null;
         
         public void run()
         {
            try
            {
               chatServer = remoteStrategy.createChat(description, owner, talkFrame, outgoingLines);
            }
            catch (ShuttingDownException sde)
            {
               SwingUtilities.invokeLater(shuttingDownDialogRunnable);
            }
            catch (RemoteConnectionException e)
            {
               log.error("Cannot create chat room: " + description);
               log.error(e);
            }
            catch (NameInUseException e)
            {
               System.out.println("Pick a new name");
            }

            Runnable r = new Runnable()
            {
               public void run()
               {
                  talkFrame.registerStrategy(new TalkConnectionStrategyImpl(owner, chatServer, outgoingLines));
                  talkFrame.show();
               }
            };

            SwingUtilities.invokeLater(r);
         }
      };

      t.start();
   }

   
   // ****************************************************************
   // JoinConnectionStrategy
   public void join(final ChatInfo chatInfo, final ChatMember newMember) throws JoinConnectionException
   {
      Thread t = new Thread()
      {
         TalkFrame talkFrame = new TalkFrame(chatInfo.get_description(), newMember.get_name(), parent);
         ReadWriteArrayList outgoingLines = new ReadWriteArrayList();
         Wrapper remoteChatServerWrapper = new Wrapper();
         ChatServer chatServer = null;
         
         public void run()
         {
            try
            {
               chatServer = remoteStrategy.join(chatInfo.get_key(), newMember, talkFrame, outgoingLines);
            }
            catch (ShuttingDownException sde)
            {
               SwingUtilities.invokeLater(shuttingDownDialogRunnable);
            }
            catch (RemoteConnectionException e)
            {
               log.error("Cannot join chat room: " + chatInfo.get_description());
               log.error(e);
            }
            catch (NameInUseException niue)
            {
               System.out.println("Pick a new name");
            }

            Runnable r = new Runnable()
            {
               public void run()
               {
                  talkFrame.registerStrategy(new TalkConnectionStrategyImpl(newMember, chatServer, outgoingLines));
                  talkFrame.registerChatKey(chatInfo.get_key());
                  talkFrame.show();
               }
            };

            SwingUtilities.invokeLater(r);
         }
      };

      t.start();
   }

   
   // ****************************************************************
   // InfoConnectionStrategy + ListConnectionStrategy
   public void getId(ChatInfo chatInfo)
   {
      joinFrame = new JoinFrame(chatInfo, this, parent);
      joinFrame.show();
   }

   public void getInfo(ArrayList chatInfoList, int key) throws ListConnectionException
   {
      infoFrame = new InfoFrame(this, (ChatInfo) chatInfoList.get(key), parent);
      infoFrame.show();
   }

   
   // ****************************************************************
   // TalkConnectionStrategy
   class TalkConnectionStrategyImpl implements TalkConnectionStrategy
   {
      private ChatServer chatServer;
      private ChatMember member;
      private ReadWriteArrayList outgoingLines;

      public TalkConnectionStrategyImpl(ChatMember member, ChatServer cs, ReadWriteArrayList outgoingLines)
      {
         chatServer = cs;
         this.member = member;
         this.outgoingLines = outgoingLines;
      }

      // getBackChat() is not currently used, since the ChatReceiverThread created by ChatManager_Impl
      // sends all existing lines in the chat room, starting with line 0.
      //      public void getBackChat(final TalkFrame talkFrame)
      //      {
      //        Thread t = new Thread() {
      //          public void run() {
      //            try {
      //              String[] backChat =  remoteChatServer.getBackChat();
      //              if (backChat == null)
      //                backChat = new String[0];
      //              talkFrame.setBackChat(backChat);
      //            }
      //            catch (ShuttingDownException sde) {
      //              SwingUtilities.invokeLater(shuttingDownDialogRunnable);
      //            }
      //            catch (RemoteConnectionException rce) {
      //              System.out.println("TalkConnectionStrategy: unable to get back chat");
      //              System.out.println(rce);
      //            }
      //          }
      //        };
      //
      //        t.start();
      //      }

      public void send(ChatMessage message) throws TalkConnectionException
      {
         outgoingLines.add(message);
      }

      public void leave()
      {
         Thread t = new Thread()
         {
            public void run()
            {
               try
               {
                  chatServer.leave(member);
               }
               catch (ShuttingDownException sde)
               {
                  SwingUtilities.invokeLater(shuttingDownDialogRunnable);
               }
               catch (RemoteConnectionException e)
               {
                  log.error("TalkConnectionStrategy.leave(): unable to leave chat room");
                  log.error(e);
               }

               chatServer = null;
            }
         };

         t.start();
      }
   }

   // ****************************************************************
   public void notifyOnClose(Component c)
   {
      c.setVisible(false);
   }

   protected boolean isChatting()
   {
      return isChatting;
   }

   protected void setIsChatting(boolean b)
   {
      isChatting = b;
   }

   // ****************************************************************

   private Runnable shuttingDownDialogRunnable = new Runnable()
   {
      public void run()
      {
         shuttingDownDialog.show();
      }
   };
}