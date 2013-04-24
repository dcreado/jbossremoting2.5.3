The servlet lease test require a web container.  Currently, this test has to be run
manually.  Here are the instructions for running
the test manually.

servlet

1. Get JBossAS and copy remoting's servlet-invoker.war (from distro or build) to the deploy directory.
2. Copy the WEB-INF/web.xml under this directory into the that of the servlet-invoker.war/WEB-INF directory.
3. Copy remoting-servlet-invoker-service.xml to deploy directory.
4. Copy jboss-remoting-tests.jar to server lib directory
5. Start web container (JBossAS).
6. Run ServletLeaseTestClient.

Even with this, will still have to look at the JBoss server.log (or just console output), where should see something like:

INFO  [STDOUT] Connection exception: null for Client org.jboss.remoting.Client@274608

every time the client lease expires on the server side.





