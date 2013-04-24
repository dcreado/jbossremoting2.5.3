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


/** 
 * AsynchInvokerCallbackHandler extends InvokerCallbackHandler with
 * asynchronous callback handling.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1633 $
 * <p>
 * Copyright Nov 28, 2006
 * </p>
 */
public interface AsynchInvokerCallbackHandler extends InvokerCallbackHandler
{
   /**
    * For push callbacks, will send the callback to the server invoker on the
    * client side, hand off processing to a separate thread, and return.<p>
    * 
    * For pull callbacks, behaves the same as handleCallback(Callback callback).<p>
    * 
    * @param callback
    * @throws HandleCallbackException
    */
   public void handleCallbackOneway(Callback callback)
   throws HandleCallbackException;
   
   
   /**
    * For push callbacks:<p>
    *   if serverSide == false, will send the callback to the server invoker on
    *   the client side, hand off processing to a separate thread, and return.<p>
    *   
    *   if serverside == true, will hand off to a separate thread the sending
    *   of the callback and will then return.<p>
    * 
    * For pull callbacks, behaves the same as handleCallback(Callback callback).<p>
    * 
    * @param callback
    * @param serverSide
    * @throws HandleCallbackException
    */
   public void handleCallbackOneway(Callback callback, boolean serverSide)
   throws HandleCallbackException;
   
   
   /**
    * For push callbacks:<p>
    *   if asynch == false, behaves the same as handleCallback(Callback callback).<p>
    *   
    *   if asynch == true:<p>
    *     if serverSide == false, will send the callback to the server invoker on
    *     the client side, hand off processing to a separate thread, and return.<p>
    *   
    *     if serverside == true, will hand off to a separate thread the sending
    *     of the callback and will then return.<p>
    * 
    * For pull callbacks, behaves the same as handleCallback(Callback callback).<p>
    * 
    * @param callback
    * @param asynch
    * @param serverSide
    * @throws HandleCallbackException
    */
   public void handleCallback(Callback callback, boolean asynch, boolean serverSide)
   throws HandleCallbackException;
}
