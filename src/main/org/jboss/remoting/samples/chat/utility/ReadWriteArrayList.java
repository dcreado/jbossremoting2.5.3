package org.jboss.remoting.samples.chat.utility;

/**
 * <p>Title: Chat4</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 * @author not attributable
 * @version 1.0
 */

import java.util.*;
import java.io.*;

public class ReadWriteArrayList implements Serializable
{
  private ArrayList arrayList;
  private static final long serialVersionUID = 6;
  private Gate gate = new Gate();

  public ReadWriteArrayList()
  {
      arrayList = new ArrayList();
  }

  public boolean add(Object o)
  {
    gate.enterWrite();
    arrayList.add(o);
    gate.leaveWrite();
    return true;
  }

  public Object get(int i)
  {
    gate.enterRead(i);
    Object o = arrayList.get(i);
    gate.leaveRead();
    return o;
  }

  public ArrayList copy()
  {
    return (ArrayList) subList(0, arrayList.size());
  }

  public List subList(int from, int to)
  {
    gate.enterRead(to - 1);
    List list = new ArrayList(arrayList.subList(from, to));
    gate.leaveRead();
    return list;
  }

  public Object firstElement()
  {
    return get(0);
  }

  public Object remove(int i)
  {
    gate.enterRead(i);
    Object o = arrayList.remove(i);
    gate.leaveRead();
    return o;
  }

  public int size()
  {
    return arrayList.size();
  }

  public Object[] toArray()
  {
    return arrayList.toArray();
  }

  public Object[] toArray(Object[] a)
  {
    return arrayList.toArray(a);
  }

  public ArrayList toArrayList()
  {
    return arrayList;
  }


////////////////////////////////////////////////////////////////////////////////
  class Gate implements Serializable
  {
    private static final long serialVersionUID = 7;

    private int numberOfReaders;
    private int numberOfWriters;

    public synchronized void enterRead(int i)
    {
      while (numberOfWriters > 0 || i > arrayList.size() - 1)
      {
        try {
          wait();
        }
        catch (InterruptedException ie) { }
      }

      numberOfReaders++;
    }

    public synchronized void enterWrite()
    {
      while (numberOfWriters > 0 || numberOfReaders > 0)
      {
        try {
          wait();
        }
        catch (InterruptedException ie) { }
      }

      numberOfWriters++;
    }

    public synchronized void leaveRead()
    {
      if (numberOfReaders <= 0)
        throw new Error("number of readers <= 0");

      numberOfReaders--;
      notifyAll();
    }

    public synchronized void leaveWrite()
    {
      if (numberOfWriters <= 0)
        throw new Error("number of writers <= 0");

      numberOfWriters--;
      notifyAll();
    }
  }
////////////////////////////////////////////////////////////////////////////////

}