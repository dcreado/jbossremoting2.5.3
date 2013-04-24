#!/bin/sh

reldir=`dirname $0`

export ANT_OPTS=-Xmx512m
#$ANT_HOME/bin/ant -lib $reldir/./lib/junit/lib/junit.jar tests.report.quick
$ANT_HOME/bin/ant -lib $reldir/./lib/junit/lib/junit.jar tests.quick tests.report.quick
