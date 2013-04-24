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

package org.jboss.test.remoting.performance;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class PerformanceReporter
{
   /**
    * Will write to report file with all the values passed.
    *
    * @param testName   Name of test run. Typically should be class and test method name.
    * @param totalCount Total number of iterations for a test (i.e. number of calls to the server).
    * @param totalTime  How long, in milliseconds, that the test took.  This is really how long it took
    *                   to make the calls to the server and not the extra time for test setup.
    * @param metadata   Any key value pairs would like added to the report.  For example, would want to
    *                   add the transport and maybe the total number of server count.
    * @throws IOException
    */
   public static void writeReport(String testName, long totalTime, int totalCount, Map metadata) throws IOException
   {
      File reportFile = new File("performance_report.txt");
      if(!reportFile.exists())
      {
         reportFile.createNewFile();
      }

      FileWriter reportWriter = new FileWriter(reportFile, true);
      reportWriter.write("\n\nTest results for test: " + testName + "\n");
      reportWriter.write("When run: " + new Date());
      reportWriter.write("\nTotal count: " + totalCount);
      reportWriter.write("\nTotal time: " + totalTime);
      if(metadata != null)
      {
         Iterator itr = metadata.keySet().iterator();
         while(itr.hasNext())
         {
            Object key = itr.next();
            Object value = metadata.get(key);
            reportWriter.write("\n" + key + " = " + value);
         }
      }
      reportWriter.write("\n\n");
      reportWriter.close();
   }
}