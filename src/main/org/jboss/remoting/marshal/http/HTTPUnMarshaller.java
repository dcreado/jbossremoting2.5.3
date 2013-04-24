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

package org.jboss.remoting.marshal.http;

import org.jboss.logging.Logger;
import org.jboss.remoting.marshal.UnMarshaller;
import org.jboss.remoting.marshal.serializable.SerializableUnMarshaller;
import org.jboss.remoting.transport.http.HTTPMetadataConstants;
import org.jboss.remoting.transport.web.WebUtil;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public class HTTPUnMarshaller extends SerializableUnMarshaller
{
   static final long serialVersionUID = 1085086661310576768L;

   public final static String DATATYPE = "http";
   
   public final static String PRESERVE_LINES = "preserveLines";

   protected final static Logger log = Logger.getLogger(HTTPUnMarshaller.class);

   /**
    * Will try to unmarshall data from inputstream.  Will try to convert to either an object
    * or a string.  If there is no data to read, will return null.
    *
    * @param inputStream
    * @param metadata
    * @param version
    * @return
    * @throws IOException
    * @throws ClassNotFoundException
    */
   public Object read(InputStream inputStream, Map metadata, int version) throws IOException, ClassNotFoundException
   {
      if (isBinaryData(metadata))
      {
         try
         {
            return super.read(inputStream, metadata, version);
         }
         catch (EOFException e)
         {
            return null;
         }
      }
      
      int contentLength = -1;
      Object ret = null;
      int bufferSize = 1024;
      byte[] byteBuffer = new byte[bufferSize];
      ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
      boolean preserveLines = false;
      boolean isChunked = false;

      // check the metadat to see if is entry for content length
      if(metadata != null)
      {
         Object value = metadata.get("Content-Length");
         if(value == null)
         {
            value = metadata.get("content-length");
         }
         if(value != null)
         {
            if(value instanceof List)
            {
               List valueList = (List) value;
               if(valueList != null && valueList.size() > 0)
               {
                  value = valueList.get(0);
               }
            }
            if(value instanceof String)
            {
               try
               {
                  contentLength = Integer.parseInt((String) value);
               }
               catch(NumberFormatException e)
               {
                  log.warn("Error converting Content-Length value (" + value + ") from metadata into int value.");
               }
            }
            else
            {
               log.warn("Can not get Content-Length from header for http unmarshalling.");
            }
         }
         
         value = metadata.get(PRESERVE_LINES);
         if (value != null)
         {
            if (value instanceof String)
            {
               preserveLines = Boolean.valueOf((String) value).booleanValue();
            }
         }
         
         value = metadata.get("transfer-encoding");
         if (value instanceof String && "chunked".equalsIgnoreCase((String)value))
         {
            isChunked = true;
         }
      }

      int pointer = 0;
      int amtRead = inputStream.read(byteBuffer);
      while(amtRead > 0)
      {
         byteOutputStream.write(byteBuffer, pointer, amtRead);
         if(!isChunked && (amtRead < bufferSize && byteOutputStream.size() >= contentLength))
         {
            //done reading, so process
            break;
         }
         amtRead = inputStream.read(byteBuffer);
      }

      byteOutputStream.flush();

      byte[] totalByteArray = byteOutputStream.toByteArray();

      if(totalByteArray.length == 0)
      {
         //nothing to read, so is null
         return null;
      }

     //boolean isError = isErrorReturn(metadata);
     //if(isBinary || isError)
      
      try
      {
         
         BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(totalByteArray)));
         StringBuffer buffer = new StringBuffer();
         
         if (preserveLines)
         {
            if (log.isTraceEnabled()) log.trace("preserving cr/lf");
            int len = 0;
            char[] chars = new char[bufferSize];
            while ((len = reader.read(chars)) > -1)
            {
               buffer.append(chars, 0, len);
            }
         }
         else
         {
            if (log.isTraceEnabled()) log.trace("deleting cr/lf");
            String str = null;
            while((str = reader.readLine()) != null)
            {
               buffer.append(str);
            }
         }
         
         reader.close();
         ret = buffer.toString();
         
      }
      catch(Exception e)
      {
         log.debug("Can not unmarshall inputstream.  Tried to unmarshall as both an object and string type.", e);
         throw new IOException("Can not unmarshall inputstream.");
      }
      
      return ret;

   }

   public UnMarshaller cloneUnMarshaller() throws CloneNotSupportedException
   {
      HTTPUnMarshaller unmarshaller = new HTTPUnMarshaller();
      unmarshaller.setClassLoader(getClassLoader());
      return unmarshaller;
   }

   private boolean isErrorReturn(Map metadata)
   {
      boolean isError = false;
      if(metadata != null)
      {
         // key of null will be the response (http type, response code, and response message)
         Object value = metadata.get(HTTPMetadataConstants.RESPONSE_CODE);
         if(value != null && value instanceof Integer)
         {
            int responseCode = ((Integer) value).intValue();
            if(responseCode > 400)
            {
               isError = true;
            }
         }
      }
      return isError;
   }

   private boolean isBinaryData(Map metadata) throws IOException
   {
      String useRemotingContentType = (String) metadata.get(HTTPMetadataConstants.USE_REMOTING_CONTENT_TYPE);
      if (Boolean.valueOf(useRemotingContentType).booleanValue())
      {
         return isBinaryDataNew(metadata);
      }
      else
      {
         return isBinaryDataOld(metadata);
      }
   }
   
   private boolean isBinaryDataOld(Map metadata) throws IOException
   {
      if (log.isTraceEnabled()) log.trace(this + " using isBinaryDataOld()");
      boolean isBinary = false;

      if(metadata != null)
      {
         // need to get the content type
         Object value = metadata.get("Content-Type");
         if(value == null)
         {
            value = metadata.get("content-type");
         }
         if(value != null)
         {
            if(value instanceof List)
            {
               List valueList = (List) value;
               if(valueList != null && valueList.size() > 0)
               {
                  value = valueList.get(0);
               }
            }
            isBinary = WebUtil.isBinary((String) value);
         }
      }
      
      if (log.isTraceEnabled()) log.trace(this + " isBinary: " + isBinary);
      return isBinary;
   }
   
   private boolean isBinaryDataNew(Map metadata) throws IOException
   {
      if (log.isTraceEnabled()) log.trace(this + " using isBinaryDataNew()");
      boolean isBinary = true;
      
      if(metadata != null)
      {
         // need to get the content type
         String remotingContentType = null;
         Object o = metadata.get(HTTPMetadataConstants.REMOTING_CONTENT_TYPE);
         if (o instanceof List)
         {
            remotingContentType = (String) ((List) o).get(0);
         }
         else if (o instanceof String)
         {
            remotingContentType = (String) o;
         }
         else 
         {
            o = metadata.get(HTTPMetadataConstants.REMOTING_CONTENT_TYPE_LC);
            if (o instanceof List)
            {
               remotingContentType = (String) ((List) o).get(0);
            }
            else if (o instanceof String)
            {
               remotingContentType = (String) o;
            }
            else if (o != null)
            {
               log.debug(this + " unrecognized remotingContentType: " + o);
            }  
         }
         
         if (log.isTraceEnabled()) log.trace(this + " remotingContentType: " + remotingContentType);
         if (remotingContentType != null)
         {
            isBinary = HTTPMetadataConstants.REMOTING_CONTENT_TYPE_NON_STRING.equals(remotingContentType);
         }
      }
      
      if (log.isTraceEnabled()) log.trace(this + " isBinary: " + isBinary);
      return isBinary;
   }

}