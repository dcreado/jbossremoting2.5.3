The servlet (and sslservlet) transport tests require a web container.  Currently, these tests have to be run
manually (JBREM-139 has been created to automate this).  Until then, here are the instructions for running
the tests manually.

*******************************************************
*****             JBREM-139 is done.              *****
*****  See tests.functional.servlet in build.xml. *****
*******************************************************

servlet

1. Get JBossAS and copy remoting's servlet-invoker.war (from distro or build) to the deploy directory.
2. Copy the WEB-INF/web.xml under this directory into the that of the servlet-invoker.war/WEB-INF directory.
3. Copy remoting-servlet-invoker-service.xml to deploy directory.
4. Copy jboss-remoting-tests.jar to server lib directory
5. Start web container (JBossAS).
6. Run *TestClient.

To run MBeanServerPlatformTestClient, uncomment the "mbeanServer" init-param in web.xml.  Also,
run JBossAS with 

# Enable the jconsole agent locally with integration of the jboss MBeans
JAVA_OPTS="$JAVA_OPTS -Djavax.management.builder.initial=org.jboss.system.server.jmx.MBeanServerBuilderImpl"
JAVA_OPTS="$JAVA_OPTS -Djboss.platform.mbeanserver"
JAVA_OPTS="$JAVA_OPTS -Dcom.sun.management.jmxremote"

added to run.bat or run.sh.  See http://wiki.jboss.org/wiki/Wiki.jsp?page=JBossMBeansInJConsole
for more information.

sslservlet

1. Steps 1-4 above, except use the WEB-INF/web.xml and remoting-servlet-invoker-service.xml that is under the
servlet/ssl directory.
2. Copy the servlet/ssl/.keystore file to the server conf directory
3. Edit jboss-web.deployer/server.xml to enable ssl connector.  It should look like following:
               
      <Connector port="8443" protocol="HTTP/1.1" SSLEnabled="true"
           maxThreads="150"
           scheme="https" secure="true" clientAuth="false"
           address="${jboss.bind.address}"
           keystoreFile="${jboss.server.home.dir}/conf/.keystore"
           keystorePass="unit-tests-server"
           sslProtocol="TLS"/> 
               

4. Start web container (JBossAS).
5. Run SSLServletInvokerTestClient or SSLServletClientAddressTestClient.



