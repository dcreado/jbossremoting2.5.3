This text covers how to run the spring http performance benchmark test.

This test can not be run automatically within remoting testsuite since requires deploying a web app.
So in order to run this test, will have to do it manually.  First step is to get a web container to deploy within.
Any web container will do, but I have been using Tomcat 5.5.17.  Am also using Spring framework 1.2.8.

Steps to run tests:

1. Run remoting build with tests.jars target.  This will build the jboss-remoting.jar and jboss-remoting-tests.jar
 that will be needed within web deployment file.
2. Create web deployment file.  This currently has to be done manually.  Create new directory and copy
src/tests/org/jboss/test/remoting/performance/spring/http/web/WEB-INF into this new directory.  Should
then have WEB-INF directory with contents applicationContext.xml, remoting-servlet.xml, and web.xml under
root directory.  Now create a lib directory under the WEB-INF directory.
Copy the jboss-remoting.jar and jboss-remoting-tests.jar into the lib directory.  Also copy all the jars under
the remoting project's lib/spring directory into this new lib directory.
Will also need to copy /lib/oswego-concurrent/lib/concurrent.jar into the lib directory.
Now zip up the WEB-INF directory into a file called remoting.war.  This will be the web deployment file.
Its contents should be:

remoting.war
- WEB-INF
  - applicationContext.xml
  - remoting-servlet.xml
  - web.xml
  - lib
    - concurrent.jar
    - jboss-remoting-tests.jar
    - jboss-remoting.jar
    - spring-aop.jar
    - spring-beans.jar
    - spring-context.jar
    - spring-core.jar
    - spring-remoting.jar
    - spring-web.jar
    - spring-webmvc.jar
    - spring.jar



3. Deploy remoting.war to web container (i.e. copy to apache-tomcat-5.5.17\webapps directory).
4. Start web container (i.e. apache-tomcat-5.5.17\bin\startup.bat/sh)
5. Run the benchmark client.  This can be done by running ant target "tests.performance.sequence.spring_http".
   The following system properties are used:
   
   - perf.seq.numofclients
   - perf.seq.numofcalls
   - perf.seq.payloadsize


