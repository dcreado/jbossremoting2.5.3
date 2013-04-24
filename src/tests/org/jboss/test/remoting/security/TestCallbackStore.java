
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
package org.jboss.test.remoting.security;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import org.apache.log4j.Logger;

public class TestCallbackStore implements TestCallbackStoreMBean
{    
   private static Logger log = Logger.getLogger(TestCallbackStore.class);
//   private static  byte[] memHolder;
   
//   static
//   {
//      long max = Runtime.getRuntime().maxMemory();
//      log.info("max mem: " + max);
//      int memSize = (int) (max * 0.6);
//      memHolder = new byte[memSize];
//      log.info("memHolder.length: " + memHolder.length);
//   }
   
   private int size;
   
   public String getStoreFilePath() {return null;}
   public String getStoreFileSuffix() {return null;}
   public void setStoreFilePath(String filePath) {}
   public void setStoreFileSuffix(String fileSuffix) {}
   public void add(Serializable object) throws IOException
   {
      if (size > 0)
         return;
      
      size++;
      log.info("TestCallbackStore received callback");
      
      synchronized (TestCallbackStore.class)
      {
         TestCallbackStore.class.notifyAll();
      }
   }
   public void create() throws Exception {log.info("create()");}
   public void destroy() {log.info("destroy()");}
   public Object getNext() throws IOException {log.info("getNext()"); return null;}
   public boolean getPurgeOnShutdown()  {log.info("getPurgeOnShutdown()"); return false;}
   public void purgeFiles() {log.info("purgeFiles()");}
   public void setConfig(Map config) {log.info("setConfig()");}
   public void setPurgeOnShutdown(boolean purgeOnShutdown) {log.info("setPurgeOnShutdown()");}
   public int size()  {log.info("size()"); return size;}
   public void start() throws Exception {log.info("start()");}
   public void stop() {log.info("stop()");}
}

