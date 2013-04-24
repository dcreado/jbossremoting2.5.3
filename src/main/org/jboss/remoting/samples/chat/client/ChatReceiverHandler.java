package org.jboss.remoting.samples.chat.client;

/**
 * <p>Title: Chat4</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 * @author not attributable
 * @version 1.0
 */


import java.util.*;

import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.invocation.RemoteInvocation;

public class ChatReceiverHandler implements InvokerCallbackHandler
{
  private TalkFrame talkFrame;

  public ChatReceiverHandler(TalkFrame tf)
  {
    talkFrame = tf;
  }
  
  public void handleCallback(Callback callback) throws HandleCallbackException
  {
     if (!(callback.getParameter() instanceof RemoteInvocation))
        throw new HandleCallbackException("invalid request format: expecting RemoteInvocation");
        
     RemoteInvocation request = (RemoteInvocation) callback.getParameter();
     String methodName = request.getMethodName();
     Object[] args = request.getParameters();

     if (methodName.equals("send"))
         send(args);
     
     else if (methodName.equals("sendMultiple"))
        sendMultiple(args);
     
     else if (methodName.equals("setKey"))
        setKey(args);
     
     else if (methodName.equals("shuttingDown"))
        shuttingDown(args);

     else
        throw new HandleCallbackException("unrecognized method name: " + methodName);
  }
  

  protected void send(Object[] args)
  {
     ChatMessage mesg = (ChatMessage) args[0];
     talkFrame.appendMessage(mesg);
  }

  protected void sendMultiple(Object[] args)
  {
     ArrayList messages = (ArrayList) args[0];
     talkFrame.appendMessages(messages);
  }
  
  protected void setKey(Object[] args)
  {
     String key = (String) args[0];
     talkFrame.registerChatKey(key);
  }
  
  protected void shuttingDown(Object[] args)
  {
  }
}
