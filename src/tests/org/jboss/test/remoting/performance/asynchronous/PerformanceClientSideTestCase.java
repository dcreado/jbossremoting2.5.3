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

package org.jboss.test.remoting.performance.asynchronous;

import org.jboss.test.remoting.performance.synchronous.PerformanceServerTest;
import org.jboss.test.remoting.performance.synchronous.PerformanceTestCase;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class PerformanceClientSideTestCase extends PerformanceTestCase
{
   public void declareTestClasses()
   {
      //**************** LOGGING ***********************
//      org.apache.log4j.BasicConfigurator.configure();
//      org.apache.log4j.Category.getRoot().setLevel(Level.DEBUG);
      //org.apache.log4j.Category.getInstance("org.jboss.remoting").setLevel(Level.DEBUG);

//      org.apache.log4j.SimpleLayout layout = new org.apache.log4j.SimpleLayout();

//      try
//      {
//         org.apache.log4j.FileAppender fileAppender = new org.apache.log4j.FileAppender(layout, "debug_output.log");
//         fileAppender.setThreshold(Level.DEBUG);
//         fileAppender.setAppend(false);
//         org.apache.log4j.Category.getRoot().addAppender(fileAppender);
//      }
//      catch(IOException e)
//      {
//         e.printStackTrace();
//      }
      //*************** END LOGGING ***********************


      String numOfClients = System.getProperty(NUMBER_OF_CLIENTS);
      if(numOfClients != null && numOfClients.length() > 0)
      {
         try
         {
            numberOfClients = Integer.parseInt(numOfClients);
         }
         catch(NumberFormatException e)
         {
            e.printStackTrace();
         }
      }

      addTestClasses(PerformanceClientSideClientTest.class.getName(),
                     numberOfClients,
                     PerformanceServerTest.class.getName());
   }

}