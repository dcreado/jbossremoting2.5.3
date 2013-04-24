The servlet (and sslservlet) transport tests require a web container.  Currently, these tests have to be run
manually (JBREM-139 has been created to automate this).  Until then, here are the instructions for running
the tests manually.

*******************************************************
*****             JBREM-139 is done.              *****
*****  See tests.functional.servlet in build.xml. *****
*******************************************************

servlet

1. Get JBossAS and copy remoting's servlet-invoker.war (from distro or build) to the deploy directory.
2. Edit the InvokerLocator attribute in remoting-servlet-invoker-service.xml, using
   a suitable set of addresses.
3. Edit ServletMultihomeTestClient.setupServer(), using the same set of addresses in the InvokerLocator.
4. Edit server.xml, giving the Connectors used by ServletMultihomeTestCase the same set of addresses.
5. Copy the WEB-INF/web.xml under this directory into the that of the servlet-invoker.war/WEB-INF directory.
6. Copy remoting-servlet-invoker-service.xml to deploy directory.
7. Build jboss-remoting-tests.jar and copy it to server lib directory.
8. Copy server.xml to deploy/jboss-web.deployer.
9. Start web container (JBossAS).
10. Run ServletMultihomeTestClient.




