package org.jboss.remoting.samples.chat.exceptions;

public class DatabaseException extends Exception implements java.io.Serializable
{
  public DatabaseException() {}
  public DatabaseException(Throwable t) { super(t); }
  public DatabaseException(String s) { super(s); }
}
