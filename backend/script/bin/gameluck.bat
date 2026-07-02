@echo off
setlocal enabledelayedexpansion

set AppName=gameluck-admin.jar
set JVM_OPTS=-Dname=%AppName% -Duser.timezone=Asia/Shanghai -Xms512m -Xmx1024m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=512m -XX:+HeapDumpOnOutOfMemoryError

echo.
echo   [1] Start %AppName%
echo   [2] Stop %AppName%
echo   [3] Restart %AppName%
echo   [4] Status %AppName%
echo   [5] Exit
echo.

set /p ID=Please choose:
if "%ID%"=="1" goto start
if "%ID%"=="2" goto stop
if "%ID%"=="3" goto restart
if "%ID%"=="4" goto status
if "%ID%"=="5" exit /b 0
exit /b 1

:start
for /f "tokens=1-2" %%a in ('jps -l ^| findstr %AppName%') do (
  set pid=%%a
  set image_name=%%b
)
if defined pid (
  echo %AppName% is already running, pid=!pid!
  exit /b 0
)
start javaw %JVM_OPTS% -jar %AppName%
echo Start %AppName% success.
exit /b 0

:stop
for /f "tokens=1-2" %%a in ('jps -l ^| findstr %AppName%') do (
  set pid=%%a
  set image_name=%%b
)
if not defined pid (
  echo %AppName% is not running.
) else (
  echo Stop %image_name%, pid=!pid!
  taskkill /f /pid !pid!
)
exit /b 0

:restart
call :stop
call :start
exit /b 0

:status
for /f "tokens=1-2" %%a in ('jps -l ^| findstr %AppName%') do (
  set pid=%%a
  set image_name=%%b
)
if not defined pid (
  echo %AppName% is not running.
) else (
  echo %image_name% is running, pid=!pid!
)
exit /b 0
