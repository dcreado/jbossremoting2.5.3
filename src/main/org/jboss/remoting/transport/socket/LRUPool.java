/*
* JBoss, Home of Professional Open Source
* Copyright 2005, JBoss Inc., and individual contributors as indicated
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


package org.jboss.remoting.transport.socket;


import java.util.ArrayList;
import java.util.Set;
import org.jboss.util.LRUCachePolicy;

/**
 * This class is an extention of LRUCachePolicy.  On a entry removal
 * it makes sure to call shutdown on the pooled ServerThread
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 */
public class LRUPool extends LRUCachePolicy
{
   private Object needEvictionLock = new Object();
   private boolean needEviction;
   
   public LRUPool(int min, int max)
   {
      super(min, max);
   }

   protected void entryRemoved(LRUCacheEntry entry)
   {
      ServerThread thread = (ServerThread) entry.m_object;
      thread.evict();
   }

   /**
    * Will evict oldest ServerThread that allows itself to be evicted.
    */
   public void evict()
   {
      // the entry will be removed by ageOut
      LRUCacheEntry entry = m_list.m_tail;
      
      while (entry != null)
      {
         ServerThread thread = (ServerThread) entry.m_object;
         
         if (thread.evict())
         {
            return;
         }
         
         entry = entry.m_prev;
      }
      
      synchronized (needEvictionLock)
      {
         needEviction = true;
      }  
   }

   public Set getContents()
   {
      return m_map.keySet();
   }

   public ArrayList getContentsByAscendingAge()
   {
      ArrayList list = new ArrayList(size());
   
      LRUCacheEntry entry = m_list.m_head;   
      while (entry != null)
      {
         list.add(entry.m_object);
         entry = entry.m_next;
      }
      
      return list;
   }
   
   public boolean getEvictionNeeded()
   {
      synchronized (needEvictionLock)
      {
         boolean answer = needEviction;
         needEviction = false;
         return answer;
      }
   }
}
