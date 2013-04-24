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
package org.jboss.remoting.callback;

import org.jboss.logging.Logger;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.serialization.SerializationStreamFactory;
import org.jboss.remoting.util.SecurityUtility;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.Map;

/**
 * Acts as a persistent list which writes Serializable objects to disk and will retrieve them
 * in same order in which they were added (FIFO).  Each file will be named according to the current
 * time (using System.currentTimeMillis() with the file suffix specified (see below).  When the
 * object is read and returned by calling the getNext() method, the file on disk for that object will
 * be deleted.  If for some reason the store VM crashes, the objects will still be available upon next startup.
 * <p/>
 * The attributes to make sure to configure are:
 * <p/>
 * file path - this determins which directory to write the objects.  The default value is the property value
 * of 'jboss.server.data.dir' and if this is not set, then will be 'data'.  For example, might
 * be /jboss/server/default/data.<p>
 * file suffix - the file suffix to use for the file written for each object stored.<p>
 * <p/>
 * This is also a service mbean, so can be run as a service within JBoss AS or stand alone.
 *
 * @author <a href="mailto:tom@jboss.org">Tom Elrod</a>
 */
public class CallbackStore implements CallbackStoreMBean
{
   private static long previousTimestamp;
   private static int timestampCounter;
   
   private String filePath = null;
   private String fileSuffix = "ser";

   private boolean isStarted = false;
   private boolean purgeOnShutdown = false;
   
   private String serializationType = "java";

   /**
    * Key for setting which directory to write the callback objects.
    * The default value is the property value of 'jboss.server.data.dir' and if this is not set,
    * then will be 'data'. Will then append 'remoting' and the callback client's session id.
    * An example would be 'data\remoting\5c4o05l-9jijyx-e5b6xyph-1-e5b6xyph-2'.
    */
   public static final String FILE_PATH_KEY = "StoreFilePath";

   /**
    * Key for setting the file suffix to use for the callback objects written to disk. The default value is "ser".
    */
   public static final String FILE_SUFFIX_KEY = "StoreFileSuffix";

   private static final Logger log = Logger.getLogger(CallbackStore.class);

   /**
    * Default store constructor.
    */
   public CallbackStore()
   {

   }

   /**
    * Store constructor.
    *
    * @param purgeOnDestroy if true, will remove all persisted objects from disk on when destroy() is called, else
    *                       will leave the files (which is the default behaviour).
    */
   public CallbackStore(boolean purgeOnDestroy)
   {
      this.purgeOnShutdown = purgeOnDestroy;
   }

   /**
    * Will get the file path value (if not already set will just use the
    * default setting) and will create the directory specified by the file path
    * if it does not already exist.
    *
    * @throws Exception
    */
   public void start() throws Exception
   {
      if (!isStarted)
      {
         // need to figure the best place to store on disk
         if (filePath == null)
         {
            try
            {
               filePath = getSystemProperty("jboss.server.data.dir", "data");
            }
            catch (Exception e)
            {
               log.debug("error", e);
               filePath = "data";
            }
         }
         File storeFile = new File(filePath);
         if (!storeFile.exists())
         {
            boolean madeDir = mkdirs(storeFile);
            if (!madeDir)
            {
               throw new IOException("Can not create directory for store.  Path given: " + filePath);
            }
         }
         isStarted = true;
      }
   }

   /**
    * Sets if store should clean up persisted files when shutdown (destroy()).
    *
    * @param purgeOnShutdown
    */
   public void setPurgeOnShutdown(boolean purgeOnShutdown)
   {
      this.purgeOnShutdown = purgeOnShutdown;
   }

   /**
    * Returns if store will clean up persisted files when shutdown (destroy()).
    *
    * @return
    */
   public boolean getPurgeOnShutdown()
   {
      return purgeOnShutdown;
   }

   /**
    * This is a no op method, but needed in order to be used as a service within JBoss AS.
    *
    * @throws Exception
    */
   public void create() throws Exception
   {
   }

   /**
    * This will allow for change of file suffix and file path and then may start again
    * using these new values.  However, any object already written out using the old
    * values will be lost as will not longer be accessible if these attributes are changed while stopped.
    */
   public void stop()
   {
      isStarted = false;
   }

   /**
    * If purgeOnDestroy is true, will remove files upon shutdown.
    */
   public void destroy()
   {
      if (purgeOnShutdown)
      {
         purgeFiles();
      }
   }

   public void purgeFiles()
   {
      String[] fileList = getObjectFileList();
      String fileToDelete = null;
      for (int x = 0; x < fileList.length; x++)
      {
         try
         {
            String separator = getSystemProperty("file.separator");
            fileToDelete = filePath + separator + fileList[x];
            final File currentFile = new File(fileToDelete);
            
            boolean deleted = ((Boolean)AccessController.doPrivileged( new PrivilegedAction()
            {
               public Object run()
               {
                  return new Boolean(currentFile.delete());
               }
            })).booleanValue();
            
            if (!deleted)
            {
               log.warn("Error purging file " + fileToDelete);
            }
         }
         catch (Exception e)
         {
            log.warn("Error purging file " + fileToDelete);
         }
      }
   }

   /**
    * Will use the values in the map to set configuration.  This will not change behaviour of store until
    * has been stopped and then started (if has not been started, will take effect upon start).
    * The keys for the map are FILE_PATH_KEY and FILE_SUFFIX_KEY.
    *
    * @param config
    */
   public void setConfig(Map config)
   {
      if (config != null)
      {
         String newFilePath = (String) config.get(FILE_PATH_KEY);
         if (newFilePath != null)
         {
            filePath = newFilePath;
         }
         String newFileSuffix = (String) config.get(FILE_SUFFIX_KEY);
         if (newFileSuffix != null)
         {
            fileSuffix = newFileSuffix;
         }
         String newSerializationType = (String) config.get(InvokerLocator.SERIALIZATIONTYPE);
         if (newSerializationType != null)
         {
            serializationType = newSerializationType;
         }
         
      }
   }

   /**
    * Gets the file path for the directory where the objects will be stored.
    *
    * @return
    */
   public String getStoreFilePath()
   {
      return filePath;
   }

   /**
    * Sets teh file path for the directory where the objects will be stored.
    *
    * @param filePath
    */
   public void setStoreFilePath(String filePath)
   {
      this.filePath = filePath;
   }

   /**
    * Gets the file suffix for each of the files that objects will be persisted to.
    *
    * @return
    */
   public String getStoreFileSuffix()
   {
      return fileSuffix;
   }

   /**
    * Sets the file suffix for each of the files that objects will be persisted to.
    *
    * @param fileSuffix
    */
   public void setStoreFileSuffix(String fileSuffix)
   {
      this.fileSuffix = fileSuffix;
   }


   /**
    * Getst the number of objects stored and available.
    *
    * @return
    */
   public int size()
   {
      verifyStarted();
      String[] objectFileList = getObjectFileList();
      if (objectFileList != null)
      {
         return objectFileList.length;
      }
      else
      {
         return 0;
      }
   }

   private void verifyStarted()
   {
      if (!isStarted)
      {
         throw new RuntimeException("Can not call upon this store method before it has been started.");
      }
   }

   /**
    * Will look through the files in the store directory for the oldest object serialized to disk, load it,
    * delete the file, and return the deserialized object.
    * Important to note that once this object is returned from this method, it is gone forever from this
    * store and will not be able to retrieve it again without adding it back.
    *
    * @return
    * @throws IOException
    */
   public Object getNext() throws IOException
   {
      verifyStarted();

      Object obj = null;
      String objectFilePath = null;

      synchronized (filePath)
      {
         String[] objectFileList = getObjectFileList();
         FileInputStream inFile = null;
         ObjectInputStream in = null;

         if (objectFileList != null && objectFileList.length > 0)
         {
            try
            {
               // only getting the first one, which will be first one entered since the getting
               // of the list is automatically ordered by the OS and all file names are numeric by time.
               String separator = getSystemProperty("file.separator");
               objectFilePath = filePath + separator + objectFileList[0];
               inFile = getFileInputStream(objectFilePath);
               in = SerializationStreamFactory.getManagerInstance(serializationType).createRegularInput(inFile);

               try
               {
                  obj = in.readObject();
               }
               catch (ClassNotFoundException e)
               {
                  throw new IOException("Error loading persisted object.  Could not load class (" + e.getMessage() + ").");
               }
            }
            finally
            {
               if (inFile != null)
               {
                  try
                  {
                     inFile.close();
                  }
                  catch (IOException ioe)
                  {
                     log.debug("Error closing FileInputStream.", ioe);
                  }
               }
               if (in != null)
               {
                  try
                  {
                     in.close();
                  }
                  catch (IOException ioe)
                  {
                     log.debug("Error closing ObjectInputStream.", ioe);
                  }
               }
               if (objectFilePath != null)
               {
                  // now remove the file
                  final File objectFile = new File(objectFilePath);
                  boolean isDeleted = ((Boolean) AccessController.doPrivileged( new PrivilegedAction()
                  {
                     public Object run()
                     {
                        return new Boolean(objectFile.delete());
                     }
                  })).booleanValue();
                  if (log.isTraceEnabled())
                  {
                     log.trace("object file (" + objectFilePath + ") has been deleted - " + isDeleted);
                  }
               }
            }
         }
      }

      return obj;
   }

   private String[] getObjectFileList()
   {
      final File storePath = new File(filePath);

      String[] objectFileList = (String[]) AccessController.doPrivileged( new PrivilegedAction()
      {
         public Object run()
         {
            return storePath.list(new StoreFileFilter());
         }
      });

      Arrays.sort(objectFileList);
      return objectFileList;
   }

   /**
    * Persists the serializable object passed to the directory specified.  The file name will be the current time
    * in milliseconds (vis System.currentTimeMillis()) with the specified suffix.  This object can later be
    * retrieved using the getNext() method, but objects will be returned in the order that they were added (FIFO).
    *
    * @param object
    * @throws IOException
    */
   public void add(final Serializable object) throws IOException
   {
      verifyStarted();

      synchronized (filePath)
      {
         long currentTimestamp = System.currentTimeMillis();
         
         if (previousTimestamp == currentTimestamp)
         {
            timestampCounter++;
         }
         else
         {
            previousTimestamp = currentTimestamp;
            timestampCounter = 0;
         }
         
         StringBuffer path = new StringBuffer(filePath);
         String separator = getSystemProperty("file.separator");
         path.append(separator).append(String.valueOf(currentTimestamp));
         path.append("-").append(timestampCounter).append(".").append(fileSuffix);
         final File storeFile = new File(path.toString());
         FileOutputStream outFile = null;
         ObjectOutputStream out = null;

         try
         {  
            outFile = getFileOutputStream(storeFile, false);
            if (serializationType.indexOf("jboss") > 0)
            {
               out = SerializationStreamFactory.getManagerInstance(serializationType).createOutput(outFile);
               out.writeObject(object);
               out.flush();
            }
            else
            {
               try
               {
                  final FileOutputStream finalOutFile = outFile;
                  out = (ObjectOutputStream) AccessController.doPrivileged( new PrivilegedExceptionAction()
                  {
                     public Object run() throws IOException
                     {
                        ObjectOutputStream out = SerializationStreamFactory.getManagerInstance(serializationType).createOutput(finalOutFile);
                        out.writeObject(object);
                        out.flush();
                        return out;
                     }
                  });
               }
               catch (PrivilegedActionException e)
               {
                  throw (IOException) e.getCause();
               }
            }
         }
         finally
         {
            if (outFile != null)
            {
               try
               {
                  outFile.close();
               }
               catch (IOException ioe)
               {
                  log.debug("Error closing FileInputStream.", ioe);
               }
            }
            if (out != null)
            {
               try
               {
                  out.close();
               }
               catch (IOException ioe)
               {
                  log.debug("Error closing ObjectInputStream.", ioe);
               }
            }

         }
      }
   }

   public class StoreFileFilter implements FilenameFilter
   {
      /**
       * Tests if a specified file should be included in a file list.
       *
       * @param dir  the directory in which the file was found.
       * @param name the name of the file.
       * @return <code>true</code> if and only if the name should be included in the file list; <code>false</code>
       *         otherwise.
       */
      public boolean accept(File dir, String name)
      {
         if (name.endsWith(fileSuffix))
         {
            return true;
         }
         else
         {
            return false;
         }
      }
   }
   
   static private boolean mkdirs(final File dir)
   {
      if (SecurityUtility.skipAccessControl())
      {
         return dir.mkdirs();
      }
      
      return ((Boolean) AccessController.doPrivileged( new PrivilegedAction()
      {
         public Object run()
         {
            return new Boolean(dir.mkdirs());
         }
      })).booleanValue();
   }
   
   static private FileInputStream getFileInputStream(final String path) throws FileNotFoundException
   {
      if (SecurityUtility.skipAccessControl())
      {
         return new FileInputStream(path);
      }
      
      try
      {
         return (FileInputStream)AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws FileNotFoundException
            {
               return new FileInputStream(path);
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (FileNotFoundException) e.getCause();
      }
   }
   
   static private FileOutputStream getFileOutputStream(final File file, final boolean append)
   throws FileNotFoundException
   {
      if (SecurityUtility.skipAccessControl())
      {
         return new FileOutputStream(file, append);
      }
      
      try
      {
         return (FileOutputStream)AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws FileNotFoundException
            {
               return new FileOutputStream(file, append);
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (FileNotFoundException) e.getCause();
      }
   }
   
   static private String getSystemProperty(final String name, final String defaultValue)
   {
      if (SecurityUtility.skipAccessControl())
         return System.getProperty(name, defaultValue);
         
      String value = null;
      try
      {
         value = (String)AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               return System.getProperty(name, defaultValue);
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (RuntimeException) e.getCause();
      }
      
      return value;
   }
   
   static private String getSystemProperty(final String name)
   {
      if (SecurityUtility.skipAccessControl())
         return System.getProperty(name);
      
      String value = null;
      try
      {
         value = (String)AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               return System.getProperty(name);
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (RuntimeException) e.getCause();
      }
      
      return value;
   }
}
