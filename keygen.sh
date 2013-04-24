#Remember to do it on JDK1.4
#Once the keys are generated in the current directory, you will need to
#move it to src/etc/org/jboss/remoting/marshall/encryption direction
#The build takes care of packaging

cygwin=false
case "`uname`" in
    CYGWIN*)
        cygwin=true
        ;;

    Darwin*)
        darwin=true
        ;;
esac
CP=output/lib/jboss-remoting.jar:lib/jboss/jboss-common.jar
# For Cygwin, switch paths to Windows format before running java
if $cygwin; then
    CP=`cygpath --path --windows "$CP"`
fi
java -cp $CP org.jboss.remoting.marshal.encryption.KeyGeneratorUtil
