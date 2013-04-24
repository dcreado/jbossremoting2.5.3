This package contains simple tests of sending invocation requests object (which contain simple string as payload)
and returning invocation response object (containing simple string as payload).  These tests use different
transports and are for comparing the processing time for the remote calls over these transports.  This client to
server call is repeated 250K times.

The package structure defines which transport type.

- remoting
   - java
      - rmi
      - socket
   - jboss
      - rmi (which is misleading as jboss serialization not working for rmi yet, so really using java serialization).
      - socket
- rmi
- socket


These tests are not automated, so will have to manually run the server and client classes for each directory.
When doing this, results should look somewhat like:

JBoss Remoting (rmi & java serialization) - 74116 milliseconds
JBoss Remoting (socket & java serialization) - 74888 milliseconds
JBoss Remoting (socket & jboss serialization) - 70041 milliseconds
RMI - 79064 milliseconds
Socket - 82869 milliseconds


Note, this is not a real world test of raw vs remoting in the sense that if were using raw transport (rmi or socket)
without using JBoss Remoting, would not have to use invocation request/response objects, but just the raw string
payload (so would obviously be faster if doing this).  These tests are to verify extra overhead of using remoting
framework vs the raw alternative, using the exact same object types as payload.