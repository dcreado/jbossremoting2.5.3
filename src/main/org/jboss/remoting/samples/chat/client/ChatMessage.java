/*
 * Created on Mar 21, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.jboss.remoting.samples.chat.client;

import java.io.Serializable;

/**
 * @author sigal
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class ChatMessage implements Serializable 
{
	public ChatMessage(String chatId, String message)
	{
		super();
		this.chatId = chatId;
		this.message = message;
	}
	
	public ChatMessage()
	{
	}

    
    private String chatId;
    public String get_chatId() { return chatId; }
    public void set_chatId(String chatId) { this.chatId = chatId; }

    private String message;
    public String get_message() { return message; }
    public void set_message(String message) { this.message = message; }
}
