This is a manual test.  To perform test, put the jboss-remoting-tests.jar in the server/all/lib directory of
the jboss server.  Then put the mbeanhandler-service.xml in the server/all/deploy directory.  Then start
jboss with the all configuration (i.e. run -c all).

This should start the MBeanHadler MBean (since it is defined as a service bean within the service.xml) and
the Connector.  Then run the ClientTest.  This should make the invocation on the MBeanHandler class.