#!/bin/sh

AppName=gameluck-admin.jar
JVM_OPTS="-Dname=$AppName -Duser.timezone=Asia/Shanghai -Xms512m -Xmx1024m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=512m -XX:+HeapDumpOnOutOfMemoryError"

if [ "$1" = "" ]; then
  echo "Usage: ./gameluck.sh {start|stop|restart|status}"
  exit 1
fi

find_pid() {
  ps -ef | grep java | grep "$AppName" | grep -v grep | awk '{print $2}'
}

start() {
  PID=$(find_pid)
  if [ "$PID" != "" ]; then
    echo "$AppName is already running, pid:$PID"
  else
    nohup java $JVM_OPTS -jar "$AppName" >/dev/null 2>&1 &
    echo "Start $AppName success."
  fi
}

stop() {
  PID=$(find_pid)
  if [ "$PID" != "" ]; then
    echo "Stop $AppName, pid:$PID"
    kill -TERM "$PID"
    while [ "$(find_pid)" != "" ]; do
      sleep 1
    done
    echo "$AppName stopped."
  else
    echo "$AppName is not running."
  fi
}

restart() {
  stop
  sleep 2
  start
}

status() {
  PID=$(find_pid)
  if [ "$PID" != "" ]; then
    echo "$AppName is running, pid:$PID"
  else
    echo "$AppName is not running."
  fi
}

case "$1" in
  start) start ;;
  stop) stop ;;
  restart) restart ;;
  status) status ;;
  *) echo "Usage: ./gameluck.sh {start|stop|restart|status}"; exit 1 ;;
esac
