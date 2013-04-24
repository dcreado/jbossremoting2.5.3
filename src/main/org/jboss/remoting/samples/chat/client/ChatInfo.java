package org.jboss.remoting.samples.chat.client;

public class ChatInfo implements java.io.Serializable
{
    public ChatInfo() {}

    public ChatInfo(String key, String description, ChatMember owner, java.util.Date origin, int size, int currentMembers)
    {
        super();
        this.key = key;
        this.description = description;
        this.owner = owner;
        this.origin = origin;
        this.size = size;
        this.currentMembers = currentMembers;
    }

    private String key;
    public String get_key() { return key; }
    public void set_key(String key) { this.key = key; }

    private String description;
    public String get_description() { return description; }
    public void set_description(String description) { this.description = description; }

    private ChatMember owner;
    public ChatMember get_owner() { return owner; }
    public void set_owner(ChatMember owner) { this.owner = owner; }

    private java.util.Date origin;
    public java.util.Date get_origin() { return origin; }
    public void set_origin(java.util.Date origin) { this.origin = origin; }

    private int size;
    public int get_size() { return size; }
    public void set_size(int size) { this.size = size; }

    private int currentMembers;
    public int get_currentMembers() { return currentMembers; }
    public void set_currentMembers(int currentMembers) { this.currentMembers = currentMembers; }
}
