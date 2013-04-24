JBoss Remoting 2.5.2.SP2
January 11, 2010

This distribution of JBoss Remoting contains the following directories:

docs - contains user guide and javadoc
etc - contains example service xml for creating and running jboss remoting services within the JBoss Application Server and example web.xml for the remoting invoker servlet.
examples - source code for remoting samples, which are referenced in the user guide.  Also contains a build file for compiling and running the samples.
lib - all the jar files needed for running remoting.  Includes the remoting jar itself, jboss-remoting.jar and the other remoting jars broken out by functionality.  Also contains servlet-invoker.war.
src - source code for JBoss Remoting.
tests - unit tests for JBoss Remoting.

Please read the JBoss_Remoting_Guide.pdf in the docs directory for more information about this release and JBoss Remoting.

Known issues:

Current issues can be found within the JBoss Remoting project on Jira (http://jira.jboss.com).  If you find a bug or issue that is not already
in Jira, please create one.

==========================================================================================================
Important features, changes, and differences in 2.5.0 release (from 2.4.0 release)

- Release 2.5.0 represents the process of upgrading the jars with which Remoting is tested and shipped. 
  In particular, the jars are now equivalent to the jars found in the JBoss Application Server version 5.0.0.CR2
  (as of 9/6/08, before its release). Changes to jbossweb (the JBoss version of Tomcat) have necessitated dropping
  the use of Apache Tomcat, which means that the "http" transport will no longer function with jdk 1.4.
  
- Other features of Remoting 2.5.0.GA should function with jdk 1.4.  However, it is the policy of JBoss, a division
  of Red Hat, no longer to support jdk 1.4.

==========================================================================================================
Important features, changes, and differences in 2.4.0 release (from 2.2.0 release)

- Support for server configuration by microcontainer injection [JBREM-63]
- Support for multihome servers [JBREM-643]
- Better support for IPv6 addresses
- Improvements in socket transport [JBREM-706], [JBREM-786], [JBREM-821], [JBREM-866]
- Improved connection monitoring [JBREM-888], [JBREM-891]
- Server gets client address in invocations
- Multiple bug fixes

==========================================================================================================
==========================================================================================================
Release Notes - JBoss Remoting - Version 2.5.3 (Flounder)

Bug

    * [JBREM-1231] - InvokerRegistry needs to have security checks for the setters

Release

    * [JBREM-1233] - Release 2.5.3

Task

    * [JBREM-1219] - Update remoting guide for "generalizeSocketException"
    * [JBREM-1229] - Display recreated InvokerLocator when bind address is 0.0.0.0
    * [JBREM-1232] - Assure version compatibility with earlier versions of Remoting


==========================================================================================================
Release Notes - JBoss Remoting - Version 2.5.2.SP3 (Flounder)

Bug

    * [JBREM-1180] - Formally reject hostnames which don't conform to RFC-952
    * [JBREM-1181] - Fix NPE in SSLSocketBuilder
    * [JBREM-1183] - ServerThread should catch java.lang.Error
    * [JBREM-1184] - Fix NPE in ClassByteClassLoader.addClass()
    * [JBREM-1185] - Change use of == to String.equals() in SSLSocketBuilder.validateStoreURL()
    * [JBREM-1188] - Socket transport doesn't set "timeout" to default value
    * [JBREM-1218] - ConnectionValidator.isValid should be volatile
    * [JBREM-1225] - Eliminate extraneous "=" from reconstructed InvokerLocator
    
Feature Request

    * [JBREM-1179] - jboss-remoting.jar should log a version message when it is loaded

Release

    * [JBREM-1223] - Release 2.5.2.SP3

Task

    * [JBREM-1163] - Evaluate FindBugs reports
    * [JBREM-1220] - Update jars to match EAP 5.1.0
    * [JBREM-1222] - Assure version compatibility with earlier versions of Remoting

==========================================================================================================
Release Notes - JBoss Remoting - Version 2.5.2.SP2 (Flounder) - Text format

Bug

    * [JBREM-1164] - Remoting shouldn't require IPv6 addresses to be surrounded by brackets
    * [JBREM-1166] - Possible race condition in ConnectionValidator
    * [JBREM-1168] - CoyoteInvoker.normalize() should handle empty URLs
    * [JBREM-1172] - SSLSocketBuilder should allow keystore and truststore URLs to be "NONE"
    * [JBREM-1175] - URL-component-based InvokerLocator constructor generates incorrect locator string under IPv6
    * [JBREM-1176] - Fix classloader leak caused by client invoker destruction delay facility

Release

    * [JBREM-1178] - Release 2.5.2.SP2

Task

    * [JBREM-1165] - Reduce log level of "received new control socket for unrecognized listenerId" message
    * [JBREM-1174] - Update jars to match EAP 5.0.1
    * [JBREM-1177] - Assure version compatibility with earlier versions of Remoting

==========================================================================================================
Release Notes - JBoss Remoting - Version 2.5.2 (Flounder)

Bug

    * [JBREM-1077] - Fix problem in CompressingMarshaller
    * [JBREM-1105] - DefaultLoadBalancer doesn't use all available server (if there are x servers available it uses x-1 of these)
    * [JBREM-1121] - Client SocketFactory should be configurable by InvokerLocator
    * [JBREM-1125] - Test for IllegalStateException when calling Timer.schedule()
    * [JBREM-1133] - CLONE [JBREM-1129] - Eliminate nondeterminism in Lease updates
    * [JBREM-1137] - New jnpserver requires wrapping call to new InitialContext() in PrivilegedExceptionAction
    * [JBREM-1145] - HTTPUnMarshaller shouldn't use the value of content-type to determine the type of an object
    * [JBREM-1147] - BisocketClientInvoker.createSocket() in callback mode should check for replaced control socket
    * [JBREM-1150] - Lease should update client list if PING invocation has same time as previous PING
    * [JBREM-1154] - Client.connect() should throw an exception when Lease creation fails
    * [JBREM-1159] - Version compatibility problem with leasing

Feature Request

    * [JBREM-1102] - Make configuration map available to MarshalFactory
    * [JBREM-1120] - Add a socket write timeout facility
    * [JBREM-1123] - SocketServerInvoker needs an immediate shutdown option
    * [JBREM-1124] - Invokers need option for configuration map parameters to override InvokerLocator parameters
    * [JBREM-1132] - CLONE [JBREM-1128] - Introduce connection identity concept
    * [JBREM-1139] - Modify PortUtil to allow a configurable range for MIN_UNPRIVILEGED_PORT and MAX_LEGAL_PORT.
    * [JBREM-1146] - Treat IOException("Connection reset by peer") as a retriable exception

Release

    * [JBREM-1135] - Release 2.5.2

Task

    * [JBREM-1134] - Assure version compatibility with earlier versions of Remoting
    * [JBREM-1136] - Update jars to match AS 5.1.0
    * [JBREM-1142] - Clarify use of "clientConnectAddress" in RemotingGuide
    * [JBREM-1151] - Correct dependency discussion in Chapter 4 of Remoting Guide
    * [JBREM-1155] - Assure version compatibility with earlier versions of Remoting
    * [JBREM-1158] - Update jars to match AS 5.2.0.Beta2

==========================================================================================================
Release Notes - JBoss Remoting - Version 2.5.1 (Flounder) - Text format
Bug

    * [JBREM-992] - Can't restart a Connector that uses SocketServerInvoker
    * [JBREM-1069] - Make ConnectorValidator configure ping period correctly
    * [JBREM-1070] - Fix deadlock in ConnectionValidator
    * [JBREM-1071] - IllegalStateException in ConnectorValidator.run()
    * [JBREM-1072] - Synchronize access to static maps in MarshalFactory
    * [JBREM-1076] - SocketServerInvoker.processInvocation() should return if running == false
    * [JBREM-1081] - Fix NPE in ServerInvokerCallbackHandler
    * [JBREM-1083] - Each Client creates a new invokerDestructionTimer
    * [JBREM-1088] - MicroSocketClientInvoker(InvokerLocator locator, Map configuration) ctor - not propagating exceptions (only message is wrapped)
    * [JBREM-1099] - Make MulticastDetector detection message send buffer size configurable
    * [JBREM-1109] - Eliminate race in MicroRemoteClientInvoker.getDataType()
    * [JBREM-1111] - CLONE [JBREM-851] - In LeasePinger replace Timer if it has shut down
    * [JBREM-1112] - Potential race between ConnectionValidator and ConnectionListener upon connection failure
    * [JBREM-1113] - ServerInvokerCallbackHandlers leak when client doesn't shut down
    * [JBREM-1116] - Remove SecurityUtility

Feature Request

    * [JBREM-1082] - Allow ConnectionValidator to access InvokerLocator parameters
    * [JBREM-1084] - Allow CallbackPoller to access Client and InvokerLocator parameters
    * [JBREM-1102] - Make configuration map available to MarshalFactory
    * [JBREM-1114] - Update servlet transport to support JBossMessaging

Release

    * [JBREM-1118] - Release 2.5.1

Task

    * [JBREM-139] - need automated test for servlet server invoker
    * [JBREM-1085] - Reduce log level of ServerSocketWrapper.close() log messages
    * [JBREM-1103] - Correct javadoc for Client.invokeOneway()
    * [JBREM-1108] - Warn against making ConnectionValidator.validatorPingPeriod shorter than ConnectionValidator.validatorPingTimeout
    * [JBREM-1110] - InvokerLocator.getParameters() should not return null
    * [JBREM-1115] - Update jars to match AS 5.1.0.CR1
    * [JBREM-1117] - Assure version compatibility with earlier versions of Remoting

==========================================================================================================
Release Notes - JBoss Remoting - Version 2.5.0.SP2 (Flounder)
Bug

    * [JBREM-1050] - HTTPClientInvoker does not support BASIC authentication for proxies when use of proxy is configured by system properties
    * [JBREM-1055] - ConnectionValidator.run() should have a sanity test to prevent calls from application code
    * [JBREM-1056] - Fix race condition in InvokerRegistry
    * [JBREM-1058] - SocketServerInvoker is missing a privileged block

Release

    * [JBREM-1053] - Release 2.5.0.SP2

Task

    * [JBREM-1057] - Fix duplicate chapers in Remoting Guide
    * [JBREM-1067] - Update dependencies to conform to Application Server 5.0.0.GA
    * [JBREM-1068] - Assure version compatibility with earlier versions of Remoting

==========================================================================================================
Release Notes - JBoss Remoting - Version 2.5.0.SP1 (Flounder)

Bug

    * [JBREM-1044] - Old version of org.apache.tomcat.util.net.SocketStatus screws up JBossWeb
    * [JBREM-1045] - ServerInvokerCallbackHandler can suffer deadlock when used with BlockingCallbackStore
    * [JBREM-1046] - HTTPClientInvoker throws NullPointerException when HttpURLConnection.getInputStream() returns null
    * [JBREM-1051] - CLONE [JBREM-1050] - HTTPClientInvoker does not support BASIC authentication for proxies when use of proxy is configured by system properties

Release

    * [JBREM-1048] - Release 2.5.0.SP1

Task

    * [JBREM-1047] - Assure version compatibility with earlier versions of Remoting
    * [JBREM-1049] - Remove old versions of jboss-remoting.jar from distribution zip file

==========================================================================================================
Release Notes - JBoss Remoting - Version 2.5.0.GA (Flounder)

Bug

    * [JBREM-1027] - CoyoteInvoker should pass URL query in InvocationRequest
    * [JBREM-1028] - JavaSerializationManager should clear ObjectOutputStream internal tables

Release

    * [JBREM-1030] - Release 2.5.0.GA

Task

    * [JBREM-1029] - Assure version compatibility with earlier versions of Remoting
    * [JBREM-1033] - Upgrade jars to versions in JBossAS 5.0.0.CR2
    * [JBREM-1034] - Assure version compatibility with earlier versions of Remoting

Sub-task

    * [JBREM-1032] - Upgrade to the latest jboss-common-core 2.2.8.GA

==========================================================================================================
Release Notes - JBoss Remoting - Version 2.4.0.SP2

Bug

    * [JBREM-1027] - CoyoteInvoker should pass URL query in InvocationRequest
    * [JBREM-1028] - JavaSerializationManager should clear ObjectOutputStream internal tables

Release

    * [JBREM-1030] - Release 2.4.0.SP2

Task

    * [JBREM-1029] - Assure version compatibility with earlier versions of Remoting

==========================================================================================================
Release Notes - JBoss Remoting - Version 2.4.0.SP1 (Pinto)

Bug

    * [JBREM-302] - remote dynamic marshall loading not working on linux
    * [JBREM-925] - SSLSocketBuilder config attribute names out of sync with docs
    * [JBREM-990] - CLONE [JBREM-960] - Remoting configured with Servlet invoker can return misleading Exceptions when Servlet path is incorrect
    * [JBREM-991] - Investigate MulticastDetector message flood
    * [JBREM-1000] - CLONE [JBREM-962] - Remote classloading does not work with Isolated EARs
    * [JBREM-1005] - Prevent build up of cancelled TimerTasks in bisocket transport
    * [JBREM-1006] - SOA MTOM bug points to bug in Remoting codebase
    * [JBREM-1019] - RemotingClassLoader needs option to delegate to user class loader before parent
    * [JBREM-1022] - Fix misleading javadoc in InvokerLocator
    * [JBREM-1023] - Dynamic marshalling fails with JBossSerialization

Feature Request

    * [JBREM-996] - CLONE [JBREM-971] - Enhance server-side connection error handling so certain (potentially revealing) socket-related exceptins are not discarded
    * [JBREM-997] - CLONE [JBREM-970] - Enhance client-side error reporting so a misspelled truststore file name required by SSL can be easily spotted
    * [JBREM-1012] - CLONE [JBREM-1010] - Add feature to declaratively turn on leasing for 2.4.0
    * [JBREM-1015] - CLONE [JBREM-1014] - Support injection of socket factory class name into AbstractInvoker

Release

    * [JBREM-1025] - Release 2.4.0.SP1

Task

    * [JBREM-999] - Make sure all fixes on 2.2 branch are ported to 2.x branch.
    * [JBREM-1017] - Improve socket timeout log message
    * [JBREM-1024] - Assure version compatibility with earlier versions of Remoting

==========================================================================================================
Release Notes - JBoss Remoting - Version 2.4.0.GA

Bug

    * [JBREM-936] - CLONE [JBREM-915] - NullPointerException in InvokerLocator
    * [JBREM-938] - CLONE [JBREM-937] - Callback BisocketServerInvoker should reuse available ServerThreads
    * [JBREM-952] - CLONE [JBREM-944] - Fix race in ConnectionNotifier
    * [JBREM-980] - ServerInvokerServlet should retrieve ServletServerInvoker based on updated InvokerLocator

Feature Request

    * [JBREM-773] - Create bisocket sample code
    * [JBREM-970] - Enhance client-side error reporting so a misspelled truststore file name required by SSL can be easily spotted
    * [JBREM-971] - Enhance server-side connection error handling so certain (potentially revealing) socket-related exceptins are not discarded
    * [JBREM-979] - Add invocation retry facility to http transport
    * [JBREM-983] - Legacy XML configuration should support multihome feature
    
Release

    * [JBREM-987] - Release 2.4.0.GA

Task

    * [JBREM-238] - JBossRemoting testsuite needs to generate artificats in the output folder
    * [JBREM-599] - remoting doc does not cover enabling or configuring leasing from the client side
    * [JBREM-816] - Add instructions to Remoting Guide for overriding a transport or create a new transport
    * [JBREM-840] - Update Remoting Guide for release 2.4.0
    * [JBREM-920] - Create build.xml target to run test suite with a Security Manager
    * [JBREM-930] - Fix chronic testsuite failures
    * [JBREM-964] - Make InvocationFailureException extend java.rmi.MarshalException
    * [JBREM-975] - Make sure all fixes on 2.2 branch are applied to 2.x branch
    * [JBREM-976] - CLONE [JBREM-912] - Remove stacktrace when SSLSocketBuilder.createSSLSocketFactory() fails
    * [JBREM-977] - Wrap MBean proxies in security conscious wrappers
    * [JBREM-978] - Put code subject to a security manager in privileged blocks, part 2
    * [JBREM-982] - Performance testing
    * [JBREM-984] - Run soak test
    * [JBREM-985] - Assure version compatibility with earlier versions of Remoting
    * [JBREM-986] - Make configurable time MicroSocketClientInvoker should wait to get connection from pool
    * [JBREM-989] - Move reference to javax.servlet.ServletException out of SecurityUtility
    

==========================================================================================================
Release Notes - JBoss Remoting - Version 2.4.0.CR2
Bug

    * [JBREM-947] - ConnectionValidator hangs when server dies
    * [JBREM-951] - CLONE [JBREM-942] - A deadlock encountered on ConnectionValidator
    * [JBREM-955] - CLONE [JBREM-954] - InterruptException should not be rethrown as CannotConnectionException
    * [JBREM-956] - Make LeasePinger timeout separately configurable
    * [JBREM-966] - CLONE [JBREM-965] - Fix PortUtil.getRandomStartingPort()

Release

    * [JBREM-968] - Release 2.4.0.CR2

Task

    * [JBREM-934] - Put code subject to a security manager in privileged blocks
    * [JBREM-953] - CLONE [JBREM-945] - Allow ServerThread to keep running after SocketTImeoutException
    * [JBREM-967] - Assure version compatibility with earlier versions of Remoting
    * [JBREM-969] - Run soak test

==========================================================================================================
Release Notes - JBoss Remoting - Version 2.4.0.CR1 (Pinto)
Bug

    * [JBREM-167] - RMI Invoker does not use true remoting marshalling/unmarshalling
    * [JBREM-677] - Compression marshalling fails intermittently.
    * [JBREM-810] - coyote.RequestMap not storing all request properties in the Map.Entry set
    * [JBREM-826] - JBoss Remoting logs at ERROR and WARN in many places
    * [JBREM-844] - Put instance variable "isRemotingUserAgent" on stack in CoyoteInvoker
    * [JBREM-901] - can't start NetworkRegistry if hostname is not resolvable
    * [JBREM-924] - Compilation errors in non ISO-8859-1 systems
    * [JBREM-927] - Adjust content length when CompressingUnMarshaller wraps HTTPUnMarshaller
    * [JBREM-932] - Check remaining time in MicroSocketClientInvoker per invocation timeout facility
    * [JBREM-933] - Fix memory leak in RemotingRMIClientSocketFactory

Feature Request

    * [JBREM-665] - Need better error reporting of response marshalling errors
    * [JBREM-764] - Wire version should be configurable per client or server

Release

    * [JBREM-935] - Release 2.4.0.CR1

Task

    * [JBREM-698] - Update Remoting Marshaller/UnMarshallers to implement PreferredStreamMarshaller/PreferredStreamUnMarshaller interfaces.
    * [JBREM-716] - Reduce log output from test suite
    * [JBREM-825] - Verify that CoyoteInvoker works with Apache Portable Runtime
    * [JBREM-876] - Get Remoting testsuite to run in hudson
    * [JBREM-899] - Verify sslservlet transport works with jbossweb
    * [JBREM-923] - Assure version compatibility with earlier versions of Remoting
    * [JBREM-931] - Create and run soak test

==========================================================================================================
Release Notes - JBoss Remoting - Version 2.4.0.Beta2 (Pinto)

Bug

    * [JBREM-739] - Fix java serialization leak.
    * [JBREM-864] - CLONE -ServerInvoker#getMBeanObjectName() returns invalid ObjectName if host value is IPv6 [JBREM-823]
    * [JBREM-877] - New Socket Connection is being Created for Every Client Request to the Server
    * [JBREM-900] - ClassCastExceptions when two apps in jboss make concurrent calls to a remote jboss
    * [JBREM-909] - Connector.stop() cannot find invoker MBean when bind address is 0.0.0.0
    * [JBREM-911] - Check out thread leak

Feature Request

    * [JBREM-703] - allow for total configuration of socket via socket invoker
    * [JBREM-865] - CLONE -Verify IPv6 addresses are handled correctly [JBREM-852]

Release

    * [JBREM-922] - Release 2.4.0.Beta2

Task

    * [JBREM-510] - SSLSocketBuilder should require a SocketFactory in server mode to have a keystore.
    * [JBREM-518] - Remove HTTPServerInvoker
    * [JBREM-521] - Organize configuration of client side socket/server socket factories and server side socket/server socket factories.
    * [JBREM-522] - When Client.addListener() creates a callback Connector for an SSL transport, the SSLServerSocket should be put in client mode.
    * [JBREM-753] - Assure version compatibility with earlier versions of Remoting
    * [JBREM-809] - Verify that the behavior of the HTTPUnMarshaller re stripping CR and LF characters is correct
    * [JBREM-842] - Deprecate multiplex transport

==========================================================================================================
Release Notes - JBoss Remoting - Version 2.4.0.Beta1 (Pinto)
Bug

    * [JBREM-166] - JMXConnectorServer will not start if using rmi invoker elsewhere
    * [JBREM-645] - Need to cleanup locatorURI parsing
    * [JBREM-653] - allow user to set content-type for http responses
    * [JBREM-675] - Problems with Servlet invoker
    * [JBREM-717] - servlet invoker illegal state exception not serializable
    * [JBREM-731] - Address of secondary server socket should be acquired each time a control connection is created.
    * [JBREM-732] - When server terminates and has clients, when the server comes back up clients that survived, can't connect. Connection refused when trying to connect the control socket.
    * [JBREM-743] - For polling callback handler, org.jboss.remoting.Client.addListener() should create only one CallbackPoller per InvokerCallbackHandler
    * [JBREM-745] - client unable to send if server recycles
    * [JBREM-747] - org.jboss.remoting.transport.Connector should unregister server invoker from MBeanServer
    * [JBREM-748] - BisocketClientInvoker should guard agains scheduling on an expired Timer
    * [JBREM-750] - Logger in HTTPClientInvoker should be static.
    * [JBREM-751] - Eliminate unnecessary "Unable to process control connection:" message from BisocketServerInvoker
    * [JBREM-752] - SSLSocket runs into BindException
    * [JBREM-754] - Reset timeout on each use of HttpURLConnection
    * [JBREM-761] - NPE in BisocketServerInvoker$ControlConnectionThread
    * [JBREM-766] - Guard against "spurious wakeup" from Thread.sleep()
    * [JBREM-769] - Sucky error message when identity creation fails
    * [JBREM-771] - MicroSocketClientInvoker can experience socket leaks
    * [JBREM-772] - MicroRemoteClientInvoker.establishLease() creates two LeasePinger TimerTasks
    * [JBREM-774] - BisocketClientInvoker.replaceControlSocket() and handleDisconnect() should close control socket
    * [JBREM-775] - MicroSocketClientInvoker.initPool() should omit pool from log message
    * [JBREM-778] - BisocketServerInvoker.start() creates a new static Timer each time
    * [JBREM-779] - BisocketClientInvoker should guard agains scheduling on an expired Timer, part 2
    * [JBREM-784] - Use separate maps for control sockets and ordinary sockets in BisocketClientInvoker
    * [JBREM-785] - BisocketClientInvoker.transport() inadvertently uses listenerId member variable
    * [JBREM-786] - stale sockets can be gotten from pool even with current rety logic
    * [JBREM-787] - Move network i/o in BisocketClientInvoker constructor to handleConnect()
    * [JBREM-788] - Access to BisocketClientInvoker static maps should be synchronized in handleDisconnect()
    * [JBREM-790] - NPE in BisocketClientInvoker$PingTimerTask
    * [JBREM-793] - Lease should synchronize access to client map
    * [JBREM-794] - LeasePinger.addClient() should not create a new LeaseTimerTask if none currently exists
    * [JBREM-806] - In HTTPClientInvoker remove newlines and carriage returns from Base64 encoded user names and passwords
    * [JBREM-811] - Privileged Block to create Class Loader
    * [JBREM-813] - ServletServerInvoker should return an exception instead of just an error message
    * [JBREM-820] - Fix race in ServerInvokerCallbackHandler.handleCallback()
    * [JBREM-821] - JBoss Remoting fails under load
    * [JBREM-822] - Avoid deadlock when Connector shuts down while callback client invoker is in handleConnect()
    * [JBREM-838] - allow user to set content-type for http responses (part 2: ServletServerInvoker)
    * [JBREM-843] - MicroSocketClientInvoker can miscount number of active sockets
    * [JBREM-846] - Fix race in JNIDDetector
    * [JBREM-851] - In LeasePinger and TimerUtil replace Timer if it has shut down
    * [JBREM-853] - ServletServerInvoker should not put null URL path in metadata map
    * [JBREM-863] - CLONE -Infinite loop in BisocketClientInvoker.createSocket [JBREM-845]
    * [JBREM-866] - CLONE -Eliminate delay in MicroSocketClientInvoker.getConnection() [JBREM-860]
    * [JBREM-870] - CLONE -MaxPoolSize value should be used in key to MicroSocketClientInvoker.connectionPools [JBREM-858]
    * [JBREM-874] - CLONE -HTTP Client invoker doesn't throw exceptions when using the sslservlet protocol [JBREM-871]
    * [JBREM-887] - ServerInvokerrCallbackHandler.createCallbackErrorHandler() inadvertently references callbackStore
    * [JBREM-888] - Client side connection exception is not thrown on the client side when the lease times out
    * [JBREM-890] - Fix thread pool eviction in socket transport
    * [JBREM-895] - MicroSocketClientInvoker.transport() must check for timeout after invocation attempt.

Feature Request

    * [JBREM-298] - Need ability to add transport specific info to invocation payload
    * [JBREM-643] - configuration for multi-homed server invokers
    * [JBREM-701] - add timeout config per client invocation for transports not descended from socket transport
    * [JBREM-728] - Improve access to HTTP response headers
    * [JBREM-746] - need to be able to tell ServerInvokerServlet to use the platform MBean server
    * [JBREM-749] - BisocketServerInvoker: Make configurable the address and port of secondary server socket
    * [JBREM-755] - Make ConnectorValidator parameters configurable
    * [JBREM-756] - CallbackPoller should shut down if too many errors occur.
    * [JBREM-757] - Implement quick Client.removeListener() for polled callbacks.
    * [JBREM-758] - Associate remote socket address with the invocation
    * [JBREM-765] - Add a separate timeout parameter for callback clients
    * [JBREM-792] - Provide to the client local address of a TCP/IP connection, as seen from the server
    * [JBREM-797] - BisocketClientInvoker.PingTimerTask should terminate more gracefully
    * [JBREM-798] - Implement quick Client.removeListener() for polled callbacks, part 2
    * [JBREM-799] - Add a separate timeout parameter for callback clients, part 2
    * [JBREM-804] - Enable HTTPClientInvoker to accept NO_THROW_ON_ERROR configuration by way of InvokerLocator
    * [JBREM-868] - CLONE -Update build.xml to allow jdk 1.5 compiler to target JVM version 1.4 (JBREM-855)
    * [JBREM-875] - CLONE -Have ServerInvokerCallbackHandler register as connection listener [JBREM-873]
    * [JBREM-891] - ConnectionValidator should report if lease has expired

Patch

    * [JBREM-781] - Socket transport needs to provide to the client local address of a TCP/IP connection, as seen from the server

Release

    * [JBREM-801] - Release 2.4.0.Beta1

Task

    * [JBREM-63] - Get rid of the legacy configuration that embeds xml parsing
    * [JBREM-228] - clustering
    * [JBREM-557] - need to include all the transports within the tests.versioning ant target
    * [JBREM-641] - re-implement the callback polling for http transport to reduce latency
    * [JBREM-687] - allow binding to 0.0.0.0
    * [JBREM-706] - In socket transport, prevent client side oneway invocations from artificially reducing concurrency
    * [JBREM-710] - Correct occasional failures of org.jboss.test.remoting.transport.socket.load.SocketLoadTestCase
    * [JBREM-713] - Check if jbossweb can replace tomcat jars for the http server invoker
    * [JBREM-715] - Don't log error for exception thrown by application in org.jboss.remoting.transport.socket.ServerThread.
    * [JBREM-730] - JNDIDetector should use rebind() instead of bind() to add new local detection messages
    * [JBREM-733] - When org.jboss.remoting.Client.addListener() creates a Connector, it should pass in InvokerLocator parameters with configuration map.
    * [JBREM-734] - BisocketClientInvoker constructor should get parameters from InvokerLocator as well as configuration map.
    * [JBREM-735] - BisocketClientInvoker.PingTimerTask should try multiple times to send ping.
    * [JBREM-741] - Eliminate unnecessary log.warn() in BisocketServerInvoker
    * [JBREM-760] - Change port for shutdown tests.
    * [JBREM-767] - Avoid deadlock in callback BisocketClientInvoker when timeout == 0
    * [JBREM-768] - Fix ShutdownTestCase failures
    * [JBREM-777] - Add quiet="true" in clean task of build.xml
    * [JBREM-782] - Remove network i/o from synch block in ServerInvokerCallbackHandler.getCallbackHandler()
    * [JBREM-783] - Remove network i/o from synch blocks that establish and terminate LeasePingers
    * [JBREM-800] - Adjust timout values to avoid cruisecontrol failures
    * [JBREM-807] - Fix failing org.jboss.test.remoting.transport.<transport>.shutdown tests

==========================================================================================================
Release Notes - JBoss Remoting - Version 2.2.0.GA (Bluto)

** Bug
    * [JBREM-721] - Fix memory leaks in bisocket transport and LeasePinger
    * [JBREM-722] - BisocketClientInvoker should start pinging on control connection without waiting for call to createsocket()
    * [JBREM-725] - NPE in BisocketServeInvoker::createControlConnection
    * [JBREM-726] - BisocketServerInvoker control connection creation needs to be in loop

** Feature Request
    * [JBREM-705] - Separate the http invoker and web container dependency
    * [JBREM-727] - Make Client's implicitly created Connectors accessible

** Task
    * [JBREM-634] - update doc on callbacks
    * [JBREM-724] - Update build.xml to create bisocket transport jars

Release Notes - JBoss Remoting - Version  2.2.0.Beta1 (Bluto)

** Bug
    * [JBREM-581] - can not do connection validation with ssl transport (only impacts detection)
    * [JBREM-600] - org.jboss.test.remoting.lease.multiplex.MultiplexLeaseTestCase fails
    * [JBREM-623] - need reset() call added back to JavaSerializationManager.sendObject() method
    * [JBREM-642] - Socket.setReuseAddress() in MicroSocketClientInvoker invocation is ignored
    * [JBREM-648] - Client.disconnect without clearing ConnectionListeners will cause NPEs
    * [JBREM-651] - Array class loading problem under jdk6
    * [JBREM-654] - a NullPointerException occures and is not handled in SocketServerInvoker and MultiplexServerInvoker
    * [JBREM-655] - rename server thread when new socket connection comes in
    * [JBREM-656] - Creating a client inside a ConnectionListener might lead into Lease reference counting problems
    * [JBREM-658] - bug in oneway thread pool under heavy load
    * [JBREM-659] - Java 6 and ClassLoader.loadClass()
    * [JBREM-670] - Remove equals() and hashCode() from org.jboss.remoting.transport.rmi.RemotingRMIClientSocketFactory.
    * [JBREM-671] - serlvet invoker no longer supports leasing
    * [JBREM-683] - ByValueInvocationTestCase is broken
    * [JBREM-685] - A server needs redundant information to detect a one way invocation
    * [JBREM-690] - Once the socket of a callback server timeouts, it starts to silently discard traffic
    * [JBREM-697] - Horg.jboss.remoting.transport.rmi.RemotingRMIClientSocketFactory.ComparableHolder should use InetAddress for host.
    * [JBREM-700] - NPE in AbstractDetector
    * [JBREM-704] - BisocketServerInvoker inadvertently logs "got listener: null" as INFO
    * [JBREM-708] - Correct org.jboss.remoting.Client.readExternal()
    * [JBREM-711] - ChunkedTestCase and Chuncked2TestCase failing
    * [JBREM-712] - HTTPInvokerProxyTestCase failing
    * [JBREM-723] - BisocketClientInvoker.transport() needs to distinguish between push and pull callback connections

** Feature Request
    * [JBREM-525] - Automatically set HostnameVerifier in HTTPSClientInvoker to allow all hosts if authorization is turned off.
    * [JBREM-598] - add timeout config per client invocation
    * [JBREM-618] - Support CallbackPoller configuration.
    * [JBREM-640] - Implement an asynchronous method for handling callbacks.
    * [JBREM-650] - Create bidirectional transport
    * [JBREM-657] - Implement versions of Client.removeListener() and Client.disconnect() which do not write to a broken server.
    * [JBREM-660] - create local transport
    * [JBREM-664] - Fix misleading InvalidConfigurationException
    * [JBREM-692] - Let marshallers/unmarshallers construct their preferred streams.
    * [JBREM-720] - Need to expose create method for TransporterClient that passes load balancing policy

** Task
    * [JBREM-274] - add callback methods to the Client API
    * [JBREM-369] - For Connectors that support callbacks on SSL connections, there should be a unified means of configuring SSLServerSocket and callback Client SSLSocket.s.
    * [JBREM-453] - Send the pre-release jar to the messaging team for testing
    * [JBREM-614] - Client.invoke() should check isConnected().
    * [JBREM-631] - Fix org.jboss.test.remoting.transport.socket.connection.SocketConnectionCheckTestCase and SocketConnectionTestCase  failures.
    * [JBREM-635] - Remove misleading error message from HTTPUnMarshaller.
    * [JBREM-636] - Remove ServerInvokerCallbackHandler's dependence on initial InvocationRequest for listerner id.
    * [JBREM-637] - add tomcat jar to component-info.xml for remoting release
    * [JBREM-644] - Reduce unit test logging output.
    * [JBREM-647] - Initialize Client configuration map to empty HashMap.
    * [JBREM-663] - Put org.jboss.remoting.LeasePinger on separate thread.
    * [JBREM-669] - Client.removeListener() should catch exception and continue if invocation to server fails.
    * [JBREM-674] - add test case for client exiting correctly
    * [JBREM-693] - Make sure "bisocket" can fully replace "socket" as Messaging's default transport
    * [JBREM-695] - RemotingRMIClientSocketFactory.createSocket() should return a socket even if a RMIClientInvoker has not been registered.
    * [JBREM-702] - http.basic.password should allow for empty passwords
    * [JBREM-707] - Fix handling of OPTIONS invocations in CoyoteInvoker
    * [JBREM-709] - Fix occasional failures of org.jboss.test.remoting.lease.socket.multiple.SocketLeaseTestCase
    * [JBREM-719] - Fix spelling of ServerInvokerCallbackHandler.REMOTING_ACKNOWLEDGES_PUSH_CALLBACKS

Release Notes - JBoss Remoting - Version 2.2.0.Alpha6

** Bug
    * [JBREM-662] - Failed ClientInvoker not cleaned up properly
    * [JBREM-673] - Use of java.util.Timer recently added and not set to daemon, so applications not exiting
    * [JBREM-683] - ByValueInvocationTestCase is broken

** Feature Request
    * [JBREM-678] - Sending an one-way invocation into a server invoker that is not started should generate a warning in logs
    * [JBREM-679] - Add the possibility to obtain ConnectionValidator's ping period from a Client
    * [JBREM-680] - An invocation into a "broken" client should throw a subclass of IOException

** Task
    * [JBREM-676] - TimerTasks run by TimerUtil should have a chance to clean up if TimerUtil.destroy() is called.

Release Notes - JBoss Remoting - Version 2.2.0.Alpha5

** Bug
    * [JBREM-666] - Broken or malicious clients can lock up the remoting server
    * [JBREM-667] - Worker thread names are confusing

** Feature Request
    * [JBREM-668] - jrunit should allow TRACE level logging


Release Notes - JBoss Remoting - Version 2.2.0.Alpha4

** Bug
    * [JBREM-649] - Concurrent exceptions on Lease when connecting/disconnecting new Clients

Release Notes - JBoss Remoting - Version 2.2.0.Alpha3 (Bluto)

** Bug
    * [JBREM-594] - invoker not torn down upon connector startup error
    * [JBREM-596] - Lease stops working if the First Client using the same Locator is closed
    * [JBREM-602] - If LeasePeriod is not set and if enableLease==true leasePeriod assumes negative value
    * [JBREM-610] - Prevent org.jboss.remoting.callback.CallbackPoller from delivering callbacks out of order.
    * [JBREM-611] - Initializing Client.sessionId outside constructor leads to java.lang.NoClassDefFoundError in certain circumstances
    * [JBREM-615] - If CallbackStore.add() is called twice quickly, System.currentTimeMillis() might not change, leading to duplicate file names.
    * [JBREM-616] - Deletion of callback files in getNext() is not synchronized, allowing callbacks to be returned multiple times.
    * [JBREM-619] - In SocketServerInvoker.run() and MultiplexServerInvoker().run, guarantee ServerSocketRefresh thread terminates.
    * [JBREM-622] - InvokerLocator already exists for listener
    * [JBREM-625] - MicroSocketClientInvoker should decrement count of used sockets when a socket is discarded.
    * [JBREM-629] - NPE in sending notification of lost client

** Feature Request
    * [JBREM-419] - Invokers Encryption
    * [JBREM-429] - Create JBossSerialization MarshalledValue more optimized for RemoteCalls
    * [JBREM-548] - Support one way invocations with no response
    * [JBREM-597] - Allow access to underlying stream in marshaller with socket transport
    * [JBREM-604] - allow socket server invoker to accept third party requests
    * [JBREM-605] - Inform a server side listener that a callback has been delivered.
    * [JBREM-607] - Add idle timeout setting for invoker threads
    * [JBREM-609] - Support nonserializable callbacks in CallbackStore

** Task
    * [JBREM-562] - publish performance benchmarks
    * [JBREM-601] - Integrate http with messaging
    * [JBREM-612] - Verify push callback connection with multiplex transport shares client to server connection.
    * [JBREM-613] - ServerInvoker.InvalidStateException should be a static class.
    * [JBREM-617] - CallbackPoller should have its own thread.
    * [JBREM-620] - If HTTPClientInvoker receives an Exception in an InvocationRespose, it should throw it instead of creating a new Exception.
    * [JBREM-621] - http transport should behave more like other transports.
    * [JBREM-624] - Add JBoss EULA
    * [JBREM-627] - Fix org.jboss.test.remoting.transport.multiplex.MultiplexInvokerShutdownTestCase failure.
    * [JBREM-630] - Fix client/server race in org.jboss.test.remoting.transport.multiplex.LateClientShutdownTestCase.
    * [JBREM-632] - Modify src/etc/log4j.xml to allow DEBUG level logging for org.jboss.remoting loggers in jrunit test cases.

Release Notes - JBoss Remoting - Version 2.0.0.GA (Boon)

** Bug
    * [JBREM-568] - SSLSocketBuilderMBean does not have matching getter/setter attribute types
    * [JBREM-569] - HTTP(S) proxy broken
    * [JBREM-576] - deadlock with socket invoker
    * [JBREM-579] - transporter does not handle reflection conversion for primitive types
    * [JBREM-580] - detection can not be used with ssl based transports
    * [JBREM-586] - socket client invoker connection pooling not bounded
    * [JBREM-590] - SSL client socket invoker does not use configuration map for SSLSocketBuilder

** Feature Request
    * [JBREM-564] - Default client socket factory configured by a system property
    * [JBREM-575] - local client invoker should convert itself to remote client invoker when being serialized

** Task
    * [JBREM-570] - Change log in ConnectionValidator to be debug instead of warn when not able to ping server
    * [JBREM-571] - fix/cleanup doc
    * [JBREM-574] - Write SSL info for virtual sockets and server sockets in toString()
    * [JBREM-578] - add spring remoting to performance benchmark tests
    * [JBREM-582] - remove System.out.println and printStackTrace calls
    * [JBREM-583] - Fix ConcurrentModificationException in MultiplexingManager.notifySocketsOfException()
    * [JBREM-584] - Get org.jboss.test.remoting.performance.spring.rmi.SpringRMIPerformanceTestCase to run with multiple clients and callback handlers
    * [JBREM-587] - ClientConfigurationCallbackConnectorTestCase(jboss_serialization) failure.
    * [JBREM-593] - Synchronize client and server in org.jboss.test.remoting.transport.multiplex.LateClientShutdownTestCase


Release Notes - JBoss Remoting - Version 2.0.0.CR1 (Boon)

** Bug
    * [JBREM-303] - org.jboss.test.remoting.transport.multiplex.BasicSocketTestCase(jboss_serialization) failure
    * [JBREM-387] - classloading problem - using wrong classloader
    * [JBREM-468] - No connection possible after an illegitimate attempt
    * [JBREM-484] - AbstractDetector.checkInvokerServer() is probably broken
    * [JBREM-494] - ClientDisconnectedException does not have serial version UID
    * [JBREM-495] - classes that do not have serial version UID
    * [JBREM-500] - ServerThread never dies
    * [JBREM-502] - not getting REMOVED notification from registry for intra-VM detection
    * [JBREM-503] - NPE in abstract detector
    * [JBREM-506] - StreamHandler throws index out of bounds exception
    * [JBREM-508] - serialization exception with mustang
    * [JBREM-519] - StreamServer never shuts down the server
    * [JBREM-526] - TimeUtil not using daemon thread
    * [JBREM-528] - ConcurrentModificationException when checking for dead servers (AbstractDetector)
    * [JBREM-530] - Detection heartbeat requires small timeout (for dead server detection)
    * [JBREM-534] - multiplex client cannot re-connect to server after it has died and then been re-started
    * [JBREM-537] - org.jboss.test.remoting.transport.rmi.ssl.handshake.RMIInvokerTestCase(java_serialization) - failing
    * [JBREM-541] - null pointer when receiving detection message
    * [JBREM-545] - setting of the bind address within MulticastDetector not working
    * [JBREM-546] - InvokerLocator.equals is broken
    * [JBREM-552] - cannot init cause of ClassCastException
    * [JBREM-553] - deadlock when disconnecting
    * [JBREM-556] - versioning tests failing
    * [JBREM-561] - http chuncked test cases failing under jdk 1.5

** Feature Request
    * [JBREM-427] - SSL Connection: load a new keystore at runtime
    * [JBREM-430] - transporter needs to be customizable
    * [JBREM-461] - Better documentation for sslmultiplex
    * [JBREM-491] - need to implement using ssl client mode for push callbacks for all transports
    * [JBREM-492] - would like an API to indicate if a transport requires SSL configuration
    * [JBREM-499] - need indication if invoker is secured by ssl
    * [JBREM-501] - give descriptive names to threads
    * [JBREM-504] - some synch blocks in AbstractDetector could change
    * [JBREM-520] - Organize configuring of ServerSocketFactory's and callback SocketFactory's.
    * [JBREM-527] - Allow user to pass Connector to be used for stream server
    * [JBREM-532] - need synchronous call from detector client to get all remoting servers on network
    * [JBREM-539] - add sslservlet procotol
    * [JBREM-544] - http client invoker (for http, https, servlet, and sslservlet) needs to handle exceptions in same manner as other transport implementations

** Task
    * [JBREM-21] - Add stress tests
    * [JBREM-218] - investigate why jrunit report on cruisecontrol inaccurate
    * [JBREM-311] - need required library matrix
    * [JBREM-320] - optimize pass by value within remoting
    * [JBREM-321] - performance tuning
    * [JBREM-368] - Configure SSLSockets and SSLServerSockets used in callbacks to be in server mode and client mode, respectively.
    * [JBREM-383] - Document new versioning for remoting
    * [JBREM-384] - correct manifest to comply with new standard
    * [JBREM-390] - finish multiplex
    * [JBREM-412] - Remoting Guide lacks left margin
    * [JBREM-423] - document how remoting identity works and how to configure
    * [JBREM-428] - add the samples/transporter/multiple/ to the distribution build (think may be there by default) and update the docs
    * [JBREM-434] - fix configuration data within document (socketTimeout should be timeout)
    * [JBREM-435] - break out remoting jars (serialization)
    * [JBREM-442] - need full doc on how socket invoker works (connection pooling, etc.)
    * [JBREM-447] - convert static transporter factory methods into constructor calls
    * [JBREM-452] - Send the pre-release jar to the messaging team for testing
    * [JBREM-454] - cache socket wrapper classes
    * [JBREM-477] - remove Client.setInvoker() and Client.getInvoker() methods
    * [JBREM-487] - Eliminate possible synchronization problem in InvokerRegistry
    * [JBREM-490] - consolidate the remoting security related classes
    * [JBREM-493] - Update version of jboss serialization being used
    * [JBREM-496] - restructure service providers for remoting
    * [JBREM-497] - change InvokerLocator to respect hostname over ip address
    * [JBREM-498] - change logging on cleaning up failed detection
    * [JBREM-507] - need to make configuration properties consistent
    * [JBREM-509] - Fix call to super() in ServerInvoker's two argument constructor.
    * [JBREM-511] - Allow HTTPSClientInvoker to create a HostnameVerifier from classname.
    * [JBREM-513] - Create SSL version of RMI transport.
    * [JBREM-514] - Fix potential NullPointerException in SSLSocketClientInvoker.createSocket().
    * [JBREM-516] - add very simple transporter sample
    * [JBREM-517] - HTTPServerInvoker needs to be deprecated
    * [JBREM-523] - connection pool on socket client invoker needs to be bound
    * [JBREM-524] - Clean up MicrosocketClientInvoker code
    * [JBREM-529] - Need to be able to reuse socket connections after move to TIME_WAIT state
    * [JBREM-533] - remove external http GET test
    * [JBREM-535] - add config to force use of remote invoker instead of local
    * [JBREM-536] - turn off host verification when doing push callback from server using same ssl config as server
    * [JBREM-538] - update remoting dist build to break out transports into individual jars
    * [JBREM-540] - need to make servlet-invoker.war part of remoting distribution
    * [JBREM-542] - change how remoting servlet finds servlet invoker
    * [JBREM-543] - fix servlet invoker error handling to be more like that of the http invoker
    * [JBREM-547] - need test case for exposing multiple interfaces for transporter server target pojo
    * [JBREM-551] - org.jboss.test.remoting.transport.multiplex.MultiplexInvokerTestCase(java_serialization) failure
    * [JBREM-555] - fix connection validator to not require extra thread to execute ping every time
    * [JBREM-558] - Break master.xml documentation into chapter files
    * [JBREM-559] - update doc for 2.0.0.CR1 release
    * [JBREM-560] - InvokerGroupTestCase(java_serialization) failure
    * [JBREM-563] - Multiplex ClientConfigurationCallbackConnectorTestCase(jboss_serialization) failure


Release Notes - JBoss Remoting - Version 2.0.0.Beta2 (Boon)

** Bug
    * [JBREM-304] - org.jboss.test.remoting.transport.multiplex.MultiplexInvokerTestCase(java_serialization) fails
    * [JBREM-371] - HTTPClientInvoker does not pass an ObjectOutputStream to the marshaller
    * [JBREM-405] - NPE when calling stop() twice on MulticastDetector
    * [JBREM-406] - StringIndexOutOfBoundsException in InvokerLocator
    * [JBREM-408] - client lease updates broken on server side
    * [JBREM-409] - Invocations fail when the pool exhausts and under heavy load
    * [JBREM-414] - JNDI detection failing
    * [JBREM-418] - ObjectInputStreamWithClassLoader can't handle primitives
    * [JBREM-426] - keyStorePath and keyStorePassword being printed to standard out
    * [JBREM-432] - TransporterClient missing serialVersionUID
    * [JBREM-440] - CallbackStore.getNext() won't necessarily get the oldest one
    * [JBREM-441] - DefaultCallbackErrorHandler.setConfig needs to avoid NPE
    * [JBREM-449] - Failure Information lost in RemotingSSLSocketFactory
    * [JBREM-450] - ClassNotFoundException for class array type during deserialization
    * [JBREM-464] - ssl socket invoker not using ssl server socket factory
    * [JBREM-467] - NPE when calling Client.removeConnectionListener()
    * [JBREM-470] - javax.net.ssl.SSLException: No available certificate corresponds to the SSL cipher suites
    * [JBREM-472] - Misspelled serialization type generates obscure NPE
    * [JBREM-479] - ClientConfigurationMapTestCase failure
    * [JBREM-482] - client invoker configuration lost after first time invoker is created

** Feature Request
    * [JBREM-312] - make TransporterClient so can be sent over network as dynamic proxy
    * [JBREM-363] - make callbacks easier with richer API for registering for callbacks
    * [JBREM-411] - Add chunked streaming support to the HTTP invoker
    * [JBREM-413] - Transporter server should allow multiple pojo targets
    * [JBREM-422] - Add plugable load balancing policy to transporter client
    * [JBREM-425] - Add support for setting the HTTP invoker content encoding that is accepted
    * [JBREM-431] - transporter server should automatically expose all interfaces implemented as subsystems
    * [JBREM-439] - StreamInvocationHandler.handleStream should throw Throwable for consistency
    * [JBREM-469] - Enable HTTP polling
    * [JBREM-471] - need better InvokerLocator.equals() implementation
    * [JBREM-481] - Changing StringUtilBuffer creation on JBossSerialization

** Task
    * [JBREM-299] - MultiplexInvokerTestCase failure
    * [JBREM-314] - need org.jboss.test.pooled.test.SSLSocketsUnitTestCase for remoting
    * [JBREM-328] - change lease ping to be HEAD instead of POST for http transport
    * [JBREM-362] - convert Connector to be standard mbean instead of xmbean
    * [JBREM-365] - set default user agent header in http client invoker
    * [JBREM-366] - clean up client invoker tracking within InvokerRegistry
    * [JBREM-367] - set live server socket factory on Connector
    * [JBREM-370] - add changes from 1.4.1 release to master.xml doc
    * [JBREM-377] - need to convert ConnectionValidator to use TimerQueue
    * [JBREM-379] - need to update jboss-serialization jar being used
    * [JBREM-380] - change ConnectionValidator to only notify once of failure
    * [JBREM-382] - disable lease ping for local invoker
    * [JBREM-415] - sync bug fixes with pooled invoker and socket invoker
    * [JBREM-420] - JNDI Detector should not need a connector when running in client mode
    * [JBREM-421] - remote stream handler api inconsistent with regular handler
    * [JBREM-436] - Extend MultiplexingInputStream with readInt() to avoid creating a MultiplexingDataInputStream in VirtualSocket.connect() and elsewhere.
    * [JBREM-437] - Eliminate "verify connect" phase from virtual socket connection protocol.
    * [JBREM-443] - add HandshakeCompletedListener support to ssl multiplex
    * [JBREM-451] - Send the pre-release jar to the messaging team for testing
    * [JBREM-455] - checking of socket connection is not really needed
    * [JBREM-456] - block callback handling when callback store full
    * [JBREM-460] - createSocket() in SSLSocketClientInvoker and SSLMultiplexClientInvoker should not assume SocketFactory has been created.
    * [JBREM-465] - property setting on the client from locator parameters and config map
    * [JBREM-476] - make externalization of Client match original instance state
    * [JBREM-478] - fix local client invoker handling of disconnected server invokers
    * [JBREM-483] - remove LocalLeaseTestCase
    * [JBREM-485] - use the ClientInvokerHolder to contain the reference counting instead of having to use clientInvokerCounter
    * [JBREM-486] - Fix ConcurrentModificationException in org.jboss.test.remoting.transport.mock.MockServerInvocationHandler


Release Notes - JBoss Remoting - Version 2.0.0.Beta1

** Bug
    * [JBREM-372] - memory leak on server side leasing
    * [JBREM-376] - problem versioning with not using connection checking
    * [JBREM-378] - client connection checking not working

** Feature Request
    * [JBREM-340] - Strong version compatibility guarantee

** Task
    * [JBREM-374] - single thread the leasing timer


Release Notes - JBoss Remoting - Version 1.4.4.GA

** Bug
    * [JBREM-426] - keyStorePath and keyStorePassword being printed to standard out


Release Notes - JBoss Remoting - Version 1.4.3.GA

** Bug
    * [JBREM-418] - ObjectInputStreamWithClassLoader can't handle primitives


Release Notes - JBoss Remoting - Version 1.4.2 final

** Feature Request
    * [JBREM-429] - Create JBossSerialization MarshalledValue more optimized for RemoteCalls


Release Notes - JBoss Remoting - Version 1.4.1 final

** Bug
    * [JBREM-313] - client lease does not work if client and server in same VM (using local invoker)
    * [JBREM-317] - HTTPClientInvoker conect sends gratuitous POST
    * [JBREM-341] - Client ping interval must be lease than lease period
    * [JBREM-343] - Exceptions on connection closing
    * [JBREM-345] - problem using client address and port
    * [JBREM-346] - fix ConcurrentModificationException in cleanup of MultiplexServerInvoker
    * [JBREM-350] - ConcurrentModificationException in InvokerRegistry
    * [JBREM-361] - Race condition in invoking on Client

** Feature Request
    * [JBREM-310] - Ability to turn connection checking off
    * [JBREM-325] - move IMarshalledValue from jboss-commons to jboss-remoting.jar

** Task
    * [JBREM-2] - sample-bindings.xml does not have entry for remoting
    * [JBREM-220] - clean up remoting wiki
    * [JBREM-316] - Maintain tomcat originated code under the ASF license.
    * [JBREM-319] - ability to inject socket factory by classname or instance in all remoting transports
    * [JBREM-323] - client lease config changes
    * [JBREM-329] - create global transport config for timeout
    * [JBREM-330] - create socket server factory based off of configuration properties
    * [JBREM-335] - Client.invoke() should pass configuration map to InvokerRegistry.createClientInvoker().
    * [JBREM-336] - InvokerRegistry doesn't purge InvokerLocators from static Set registeredLocators.
    * [JBREM-337] - PortUtil.findFreePort() should return ports only between 1024 and 65535.
    * [JBREM-342] - Thread usage for timers and lease functionality
    * [JBREM-354] - ServerInvokerCallbackHandler should make its subsystem accessible.
    * [JBREM-356] - ServerInvoker should destroy its callback handlers.
    * [JBREM-359] - MultiplexInvokerConfigTestCase should execute MultiplexInvokerConfigTestServer instead of MultiplexInvokerTestServer.


Release Notes - JBoss Remoting - Version 1.4.0 final

** Feature Request
    * [JBREM-91] - UIL2 type transport (duplex calling of same socket)
    * [JBREM-117] - clean up callback client after several failures delivering callbacks
    * [JBREM-138] - HTTP/Servlet invokers require content length to be set
    * [JBREM-229] - Remove dependency on ThreadLocal for SerializationManagers and pluggable serialization
    * [JBREM-233] - Server side exception listeners for client connections
    * [JBREM-257] - Append client stack trace to thrown remote exception
    * [JBREM-261] - Integration with IMarshalledValue from JBossCommons
    * [JBREM-278] - remoting detection needs ability to accept detection of server invoker running locally
    * [JBREM-280] - no way to add path to invoker uri when using complex configuration

** Bug
    * [JBREM-41] - problem using localhost/127.0.0.1
    * [JBREM-115] - http server invoker does not wait to finish processing on stop
    * [JBREM-223] - Broken Pipe if client don't do any calls before the timeout value
    * [JBREM-224] - java.net.SocketTimeoutException when socket timeout on the keep alive
    * [JBREM-231] - bug in invoker locator when there are no params (NPE)
    * [JBREM-234] - StreamCorruptedException in DTM testcase
    * [JBREM-240] - TestUtil does not always give free port for server
    * [JBREM-243] - socket client invoker sharing pooled connections
    * [JBREM-250] - InvokerLocator doesn't support URL in IPv6 format (ex: socket://3000::117:5400/)
    * [JBREM-251] - transporter passes method signature based on concrete object and not the parameter type
    * [JBREM-256] - NullPointer in MarshallerLoaderHandler.java:69
    * [JBREM-259] - Unmarshalling of server response is not using caller's classloader
    * [JBREM-271] - http client invoker needs to explicitly set the content type if not provided
    * [JBREM-277] - error shutting down coyote invoker when using APR protocol
    * [JBREM-281] - getting random port for connectors is not reliable
    * [JBREM-282] - ServletServerInvoker not working with depployed for use as ejb invoker
    * [JBREM-286] - Socket server does not clean up server threads on shutdown
    * [JBREM-289] - PortUtil only checking for free ports on localhost

** Task
    * [JBREM-7] - Add more tests for local invoker
    * [JBREM-121] - improve connection failure callback
    * [JBREM-126] - add tests for client vs. server address bindings
    * [JBREM-195] - Performance optimization
    * [JBREM-199] - remoting clients required to include servlet-api.jar
    * [JBREM-207] - clean up build file
    * [JBREM-214] - multiplex performance tests getting out of memory error
    * [JBREM-215] - re-write http transport/handler documentation
    * [JBREM-216] - Need to add new samples to example build in distro
    * [JBREM-217] - create samples documentation
    * [JBREM-219] - move remoting site to jboss labs
    * [JBREM-226] - Release JBoss Remoting 1.4.0 final
    * [JBREM-230] - create interface for marshallers to implement for swapping out serialization impl
    * [JBREM-235] - add new header to source files
    * [JBREM-239] - Update the LGPL headers
    * [JBREM-242] - Subclass multiplex invoker from socket invoker.
    * [JBREM-249] - http invoker (tomcat connector) documentation
    * [JBREM-253] - Convert http server invoker implementation to use tomcat connector and protocols
    * [JBREM-255] - HTTPClientInvoker not setting response code or message
    * [JBREM-275] - fix package error in examle-service.xml
    * [JBREM-276] - transporter does not throw original exception from server implementation
    * [JBREM-279] - socket server invoker spits out error messages on shutdown when is not needed
    * [JBREM-287] - need to complete javadoc for all user classes/interfaces
    * [JBREM-288] - update example-service.xml with new configurations

** Reactor Event
    * [JBREM-241] - Refactor SocketServerInvoker so that can be subclassed by MultiplexServerInvoker


Release Notes - JBoss Remoting - Version 1.4.0 beta

** Feature Request
    * [JBREM-28] - Marshaller for non serializable objects
    * [JBREM-40] - Compression marshaller/unmarshaller
    * [JBREM-120] - config for using hostname in locator url instead of ip
    * [JBREM-140] - can not set response headers from invocation handlers
    * [JBREM-148] - support pluggable object serialization packages
    * [JBREM-175] - Remove Dependencies to Server Classes from UnifiedInvoker
    * [JBREM-180] - add plugable serialization
    * [JBREM-187] - Better HTTP 1.1 stack support for HTTP invoker
    * [JBREM-201] - Remove dependency from JBossSerialization

** Bug
    * [JBREM-127] - RMI Invoker will not bind to specified address
    * [JBREM-192] - distro contains samples in src and examples directory
    * [JBREM-193] - HTTPClientInvoker doesn't call getErrorStream() on HttpURLConnection when an error response code is returned
    * [JBREM-194] - multiplex performance tests hang
    * [JBREM-202] - getUnmarshaller always calls Class.forName operation for creating Unmarshallers
    * [JBREM-203] - rmi server invoker hangs if custom unmarshaller
    * [JBREM-205] - Spurious java.net.SocketException: Connection reset error logging
    * [JBREM-210] - InvokerLocator should be insensitive to parameter order

** Task
    * [JBREM-9] - Fix performance tests
    * [JBREM-33] - Add GET support within HTTP server invoker
    * [JBREM-145] - convert user guide from MS word doc to docbook
    * [JBREM-182] - Socket timeout too short (and better error message)
    * [JBREM-183] - keep alive support for http invoker
    * [JBREM-196] - reducde the number of retries for socket client invoker
    * [JBREM-204] - create complex remoting example using dynamic proxy to endpoint
    * [JBREM-212] - create transporter implementation
    * [JBREM-213] - allow config of ignoring https host validation (ssl) via metadata


** Patch
    * [JBREM-152] - NullPointerException in SocketServerInvoker.stop() at line 185.
    * [JBREM-153] - LocalClientInvoker's outlive their useful lifetime, causing anomalous behavior


Release Notes - JBoss Remoting - Version 1.2.0 final

** Feature Request
    * [JBREM-8] - Ability to stream files via remoting
    * [JBREM-22] - Manipulation of the client proxy interceptor stack
    * [JBREM-24] - Allow for specific network interface bindings
    * [JBREM-27] - Support for HTTP/HTTPS proxy
    * [JBREM-35] - Servlet Invoker - counterpart to HTTP Invoker (runs within web container)
    * [JBREM-43] - custom socket factories
    * [JBREM-46] - Connection failure callback
    * [JBREM-87] - Add handler metadata to detection messages
    * [JBREM-93] - Callback handler returning a generic Object
    * [JBREM-94] - callback server specific implementation
    * [JBREM-109] - Add support for JaasSecurityDomain within SSL support
    * [JBREM-122] - need log4j.xml in examples

** Bug
    * [JBREM-58] - Bug with multiple callback handler registered with same server
    * [JBREM-64] - Need MarshalFactory to produce new instance per get request
    * [JBREM-84] - Duplicate Connector shutdown using same server invoker
    * [JBREM-92] - in-VM push callbacks don't  work
    * [JBREM-97] - Won't compile under JDK 1.5
    * [JBREM-108] - can not set bind address and port for rmi and http(s)
    * [JBREM-114] - getting callbacks for a callback handler always returns null
    * [JBREM-125] - can not configure transport, port, or host for the stream server
    * [JBREM-131] - invoker registry not update if server invoker changes locator
    * [JBREM-134] - can not remove callback listeners from multiple callback servers
    * [JBREM-137] - Invalid RemoteClientInvoker reference maintained by InvokerRegistry after invoker disconnect()
    * [JBREM-141] - bug connecting client invoker when client detects that previously used one is disconnected
    * [JBREM-143] - NetworkRegistry should not be required for detector to run on server side

** Task
    * [JBREM-11] - Create seperate JBoss Remoting module in CVS
    * [JBREM-20] - break out remoting into two seperate projects
    * [JBREM-34] - Need to add configuration properties for HTTP server invoker
    * [JBREM-39] - start connector on new thread
    * [JBREM-55] - Clean up Callback implementation
    * [JBREM-57] - Remove use of InvokerRequest in favor of Callback object
    * [JBREM-62] - update UnifiedInvoker to use remote marshall loading
    * [JBREM-67] - Add ability to set ThreadPool via configuration
    * [JBREM-98] - remove isDebugEnabled() within code as is now depricated
    * [JBREM-101] - Fix serialization versioning between releases of remoting
    * [JBREM-104] - Release JBossRemoting 1.1.0
    * [JBREM-110] - create jboss-remoting-client.jar
    * [JBREM-113] - Convert remote tests to use JRunit instead of distributed test framework
    * [JBREM-123] - update detection samples
    * [JBREM-128] - standardize address and port binding configuration for all transports
    * [JBREM-130] - updated wiki for checkout and build
    * [JBREM-132] - write test case for JBREM-131
    * [JBREM-133] - Document use of Client (as a session object)
    * [JBREM-135] - Remove ClientInvokerAdapter

** Reactor Event
    * [JBREM-65] - move callback specific classes into new callback package
    * [JBREM-111] - pass socket's output/inputstream directly to marshaller/unmarshaller


Release Notes - JBoss Remoting - Version 1.0.2 final

** Bug
    * [JBREM-36] - performance tests fail for http transports
    * [JBREM-66] - Race condition on startup
    * [JBREM-82] - Bad warning in Connector.
    * [JBREM-88] - HTTP invoker only binds to localhost
    * [JBREM-89] - HTTPUnMarshaller finishing read early
    * [JBREM-90] - HTTP header values not being picked up on the http invoker server

** Task
    * [JBREM-70] - Clean up build.xml. Fix .classpath and .project for eclipse
    * [JBREM-83] - Updated Invocation marshalling to support standard payloads


Release Notes - JBoss Remoting - Version 1.0.1 final

** Feature Request
    * [JBREM-54] - Need access to HTTP response headers

** Bug
    * [JBREM-1] - Thread.currentThread().getContextClassLoader() is wrong
    * [JBREM-31] - Exception handling in http server invoker
    * [JBREM-32] - HTTP Invoker - check for threading issues
    * [JBREM-50] - Need ability to set socket timeout on socket client invoker
    * [JBREM-59] - Pull callback collection is unbounded - possible Out of Memory
    * [JBREM-60] - Incorrect usage of debug level logging
    * [JBREM-61] - Possible RMI exception semantic regression

** Task
    * [JBREM-15] - merge UnifiedInvoker from remoting branch
    * [JBREM-30] - Better integration for registering invokers with MBeanServer
    * [JBREM-37] - backport to 4.0 branch before 1.0.1 final release
    * [JBREM-56] - Add Callback object instead of using InvokerRequest

** Reactor Event
    * [JBREM-51] - defining marshaller on remoting client


Release Notes - JBoss Remoting - Version 1.0.1 beta

** Bug
    * [JBREM-19] - Try to reconnect on connection failure within socket invoker
    * [JBREM-25] - Deadlock in InvokerRegistry

** Feature Request
    * [JBREM-12] - Support for call by value
    * [JBREM-26] - Ability to use MBeans as handlers

** Task
    * [JBREM-3] - Fix Asyn invokers - currently not operable
    * [JBREM-4] - Added test for throwing exception on server side
    * [JBREM-5] - Socket invokers needs to be fixed
    * [JBREM-16] - Finish HTTP Invoker
    * [JBREM-17] - Add CannotConnectException to all transports
    * [JBREM-18] - Backport remoting from HEAD to 4.0 branch


** Reactor Event
    * [JBREM-23] - Refactor Connector so can configure transports
    * [JBREM-29] - Over load invoke() method in Client so metadata not required
