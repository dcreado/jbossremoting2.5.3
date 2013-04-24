This package directory contains client/server classes for testing concurrent invocations through the
socket transport.  These are not included as part of standard testsuite run and need to be run manually.

6/29/06 - Ran with 250 threads on client, each looping 1M times making server invocations.  The test
itself finally timed out after two hours of running (per code in test classes), but was able over 35K invocations
per thread without error or leak in memory.