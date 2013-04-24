package org.jboss.remoting.samples.chat.client;

public class ChatMember implements java.io.Serializable
{
    public ChatMember() {}

    public ChatMember(String name)
    {
        super();
        this.name = name;
    }

    private String name;
    public String get_name() { return name; }
    public void set_name(String name) { this.name = name; }

}
