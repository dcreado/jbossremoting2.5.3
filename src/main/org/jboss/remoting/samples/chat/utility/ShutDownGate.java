package org.jboss.remoting.samples.chat.utility;

/**
 * <p>Title: Chat4</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 * @author not attributable
 * @version 1.0
 */

import java.io.Serializable;

import org.jboss.remoting.samples.chat.exceptions.ShuttingDownException;

public class ShutDownGate implements Serializable {

    private static final long serialVersionUID = 2;

    private int numberOfUsers;
    private boolean shuttingDown;

    public ShutDownGate()
    {
      reset();
    }

    public void reset()
    {
      numberOfUsers = 0;
      shuttingDown = false;
    }

    public synchronized void check() throws ShuttingDownException
    {
      if (shuttingDown == true)
        throw new ShuttingDownException();
    }

    public synchronized boolean isShuttingDown()
    {
      return shuttingDown;
    }

    public synchronized void enter() throws ShuttingDownException
    {
      if (shuttingDown == true)
        throw new ShuttingDownException();
      numberOfUsers++;
    }

    public synchronized void leave()
    {
      if (numberOfUsers <= 0)
        throw new Error("ShutDownGate: number of Users <= 0");

      if (--numberOfUsers == 0)
        notifyAll();
    }

    public synchronized void shutDown()
    {
      shuttingDown = true;

      while (numberOfUsers > 0) {
        try {
          System.out.println("shutdown(): numberOfUsers == " + numberOfUsers);
          wait();
        }
        catch (InterruptedException ie) {}
      }
    }

  }