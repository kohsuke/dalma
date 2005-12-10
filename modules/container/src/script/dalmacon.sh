#!/bin/sh

cygwin=false;
case "`uname`" in
  CYGWIN*) cygwin=true ;;
esac

if [ -z "$DALMA_HOME" ]
then
  bindir=`dirname $0`
  DALMA_HOME=`dirname $bindir`
fi

if $cygwin
then
  DALMA_HOME=`cygpath -w "$DALMA_HOME"`
fi

exec java "-DDALMA_HOME=$DALMA_HOME" -jar "$DALMA_HOME/bin/container.jar" "$@"
