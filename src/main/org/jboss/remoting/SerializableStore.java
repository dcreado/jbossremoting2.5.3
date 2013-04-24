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
package org.jboss.remoting;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

/**
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public interface SerializableStore
{
   /**
    * Getst the number of objects stored and available.
    *
    * @return
    */
   int size();

   /**
    * Will look through the files in the store directory for the oldest object serialized to disk, load it,
    * delete the file, and return the deserialized object.
    * Important to note that once this object is returned from this method, it is gone forever from this
    * store and will not be able to retrieve it again without adding it back.
    *
    * @return
    * @throws java.io.IOException
    */
   Object getNext() throws IOException;

   /**
    * Persists the serializable object passed to the directory specified.  The file name will be the current time
    * in milliseconds (vis System.currentTimeMillis()) with the specified suffix.  This object can later be
    * retrieved using the getNext() method, but objects will be returned in the order that they were added (FIFO).
    *
    * @param object
    * @throws java.io.IOException
    */
   void add(Serializable object) throws IOException;

   /**
    * Will use the values in the map to set configuration.
    *
    * @param config
    */
   void setConfig(Map config);

   /**
    * Will get the file path value (if not already set will just use the
    * default setting) and will create the directory specified by the file path
    * if it does not already exist.
    *
    * @throws Exception
    */
   void start() throws Exception;

   /**
    * This will allow for change of file suffix and file path and then may start again
    * using these new values.  However, any object already written out using the old
    * values will be lost as will not longer be accessible if these attributes are changed while stopped.
    */
   void stop();

   /**
    * This is a no op method, but needed in order to be used as a service within JBoss AS.
    *
    * @throws Exception
    */
   void create() throws Exception;

   /**
    * This is a no op method, but needed in order to be used as a service within JBoss AS.
    */
   void destroy();

   /**
    * Sets if store should clean up persisted files when shutdown (destroy()).
    *
    * @param purgeOnShutdown
    */
   void setPurgeOnShutdown(boolean purgeOnShutdown);

   /**
    * Returns if store will clean up persisted files when shutdown (destroy()).
    *
    * @return
    */
   boolean getPurgeOnShutdown();

   void purgeFiles();
}
