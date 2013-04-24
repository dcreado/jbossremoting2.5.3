/*
 * JBoss, Home of Professional Open Source
 * Copyright 2006, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.remoting.transport.multiplex.utility;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.jboss.logging.Logger;


/**
 * @author <a href="mailto:ron.sigal@jboss.com">Ron Sigal</a>
 * @version $$Revision$$
 * <p>
 * Copyright (c) April 8, 2006
 * </p>
 */

/**
 * <code>VirtualSelector</code> is a simple version of
 * <code>java.nio.channels.Selector</code>.  It allows a thread to 
 * register for <code>InputStream</code>s to notify it that they have bytes ready to read.
 * 
 * @deprecated As of release 2.4.0 the multiplex transport will no longer be actively supported.
 */
public class VirtualSelector
{  
   protected static final Logger log = Logger.getLogger(VirtualSelector.class);
   private Map attachmentMap    = new HashMap();
   private Map readyMap         = new HashMap();
   private Map readyMapCopy     = new HashMap();
   private Set removableStreams = new HashSet();
   
   private boolean open = true;
   private boolean closed;
   private boolean inUse;
   
   
/**
 * Allows an <code>InputStream</code> to register itself and an attachment.
 * @param inputStream
 * @param attachment
 */
   public synchronized void register(GrowablePipedInputStream inputStream, Object attachment)
   {
      attachmentMap.put(inputStream, attachment);
   }
   
   
/**
 * Allows an <code>InputStream</code> to unregister itself.
 * @param inputStream
 */
   public synchronized void unregister(InputStream inputStream)
   {
      attachmentMap.remove(inputStream);
      readyMap.remove(inputStream);
      removableStreams.add(inputStream);
   }
   
   
/**
 * Allows a <code>Thread</code> to wait to be informed of InputStreams that have bytes ready to read.
 * @return a <code>Map</code> from a set of <code>InputStream</code>s with ready bytes to their attachments.
 *         If <code>close()</code> is called while a <code>Thread</code> is waiting in
 *         <code>select()</code>, and if there are
 *         no ready <code>InputStreams</code>, <code>select()</code> will return null.
 */
   public synchronized Map select()
   {
      if (closed)
         return null;
      
      // Remove any InputStreams declared to be removable.
      Iterator it = removableStreams.iterator();
      while (it.hasNext())
      {
         readyMapCopy.remove(it.next());
      }
      removableStreams.clear();
      
      // Add any newly ready InputStreams.
      readyMapCopy.putAll(readyMap);
      readyMap.clear();
      
      // If there are any ready InputStreams, we're done.
      if (!readyMapCopy.isEmpty())
         return readyMapCopy;
         
      // Otherwise, wait for some InputStream to become ready.
      while (readyMap.isEmpty() && open)
      {
         try
         {
            inUse = true;
            wait();
         }
         catch (InterruptedException ignored) {}
         finally
         {
            inUse = false;
         }
      }
      
      // If readyMap is empty, then we're here because someone called close().
      if (readyMap.isEmpty())
      {
         log.debug("returning null");
         finishClose();
         return null;
      }
      
      // Transfer all ready InputStreams to readyMapCopy.
      readyMapCopy.putAll(readyMap);
      readyMap.clear();
      return readyMapCopy;
   }
   
   
/**
 * Returns true if and only if this <code>VirtualSelector</code> is open.
 * @return true if and only if this <code>VirtualSelector</code> is open
 */
   public boolean isOpen()
   {
      return open;
   }
   
   
/**
 * Marks this <code>VirtualSelector</code> as preparing to close.
 * If any <code>Thread</code> is blocked in <code>select()</code>, <code>select()</code> returns null.
 */
   public synchronized void close()
   {
      open = false;
      
      if (inUse)
         notifyAll();
      else
         finishClose();
   }
   
   
/**
 * Allows an <code>InputStream</code> to inform a listening <code>Thread</code>
 * that it has bytes ready to read.
 * @param inputStream
 */
   public synchronized void addToReadyInputStreams(InputStream inputStream)
   throws IOException
   {
      if (!open)
         throw new IOException("This VirtualSelector is closed.");
      
      readyMap.put(inputStream, attachmentMap.get(inputStream));
      notifyAll();
   }
   
   
/**
 * Indicates that an <code>InputStream</code> has been processed.
 * If <code>InputStream</code> has no available bytes, it
 * will be removed from the <code>Set</code> of <code>InputStream</code>s
 * that will be returned by the next call to
 * <code>select()</code>.  If <code>InputStream</code> has available bytes,
 * its status will not be changed.
 * 
 * @param inputStream
 * @throws IOException
 */
   public synchronized void remove(InputStream inputStream) throws IOException
   {
      removableStreams.add(inputStream);
   }
   
   
   public synchronized void waitUntilEmpty()
   {
      while (!readyMap.isEmpty())
      {
         try
         {
            log.debug("waiting until empty");
            wait();
         }
         catch (InterruptedException ignored) {}
      }
      log.debug("empty");
   }
   
   
/**
 * Finishes the process of closing this <code>VirtualSelector</code>, releasing all resources.
 */
   protected void finishClose()
   {
      attachmentMap.clear();
      readyMap.clear();
      readyMapCopy.clear();
      removableStreams.clear();
      closed = true;
   }
}