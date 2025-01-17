#!/bin/sh
# Adapted from tlcockpit.sh to ensure the script works with cygwin

scriptname=`basename "$0" .sh`
progname=${scriptname%"-gui"}
jar="$progname.jar"
jarpath=`kpsewhich --progname="$progname" --format=texmfscripts "$jar"`

kernel=`uname -s 2>/dev/null`
if echo "$kernel" | grep CYGWIN >/dev/null; then
  CYGWIN_ROOT=`cygpath -w /`
  export CYGWIN_ROOT
  jarpath=`cygpath -w "$jarpath"`
fi

splashimage=`dirname "${jarpath}"`
splashimage+="/datatooltk-splash.png"

if [ -f $splashimage ]; then
 exec java "-splash:$splashimage" -jar "$jarpath" --gui "$@"
else
 exec java -jar "$jarpath" --gui "$@"
fi

