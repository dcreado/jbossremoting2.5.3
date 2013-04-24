package org.jboss.remoting.marshal;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;


/**
 * A PreferedStreamMarshaller can create from a raw OutputStream the
 * particular OutputStream it prefers to use.
 *
 * @author <a href="mailto:ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 2000 $
 * <p>
 * Copyright (c) Jan 26, 2007
 * </p>
 */

public interface PreferredStreamMarshaller extends SerialMarshaller
{
   /**
    * An application that calls getMarshallingStream() should provide a
    * basic OutputStream, e.g., SocketOutputStream, which can be wrapped
    * to provide the facilities desired by the PreferredStreamMarshaller. 
    * 
    * @param outputStream a raw OutputStream
    * @return the OutputStream to be used for marshalling
    * @throws IOException if it unable to create OutputStream
    */
   OutputStream getMarshallingStream(OutputStream outputStream) throws IOException;
   
   /**
    * An application that calls getMarshallingStream() should provide a
    * basic OutputStream, e.g., SocketOutputStream, which can be wrapped
    * to provide the facilities desired by the PreferredStreamMarshaller. 
    * 
    * @param outputStream a raw OutputStream
    * @param config a Map with configuration information (e.g., serialization type)
    * @return the OutputStream to be used for marshalling
    * @throws IOException if it unable to create OutputStream
    */
   OutputStream getMarshallingStream(OutputStream outputStream, Map config) throws IOException;
}