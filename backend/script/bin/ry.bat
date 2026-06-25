rem 浣跨敤鑰呭簲鏍规嵁鑷韩骞冲彴缂栫爜鑷杞崲 闃叉涔辩爜 渚嬪 win浣跨敤gbk缂栫爜
@echo off

rem jar骞崇骇鐩綍
set AppName=gameluck-admin.jar

rem JVM鍙傛暟
set JVM_OPTS="-Dname=%AppName%  -Duser.timezone=Asia/Shanghai -Xms512m -Xmx1024m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=512m -XX:+HeapDumpOnOutOfMemoryError -XX:+UseZGC"


ECHO.
	ECHO.  [1] 鍚姩%AppName%
	ECHO.  [2] 鍏抽棴%AppName%
	ECHO.  [3] 閲嶅惎%AppName%
	ECHO.  [4] 鍚姩鐘舵€?%AppName%
	ECHO.  [5] 閫€ 鍑?
ECHO.

ECHO.璇疯緭鍏ラ€夋嫨椤圭洰鐨勫簭鍙?
set /p ID=
	IF "%id%"=="1" GOTO start
	IF "%id%"=="2" GOTO stop
	IF "%id%"=="3" GOTO restart
	IF "%id%"=="4" GOTO status
	IF "%id%"=="5" EXIT
PAUSE
:start
    for /f "usebackq tokens=1-2" %%a in (`jps -l ^| findstr %AppName%`) do (
		set pid=%%a
		set image_name=%%b
	)
	if  defined pid (
		echo %%is running
		PAUSE
	)

start javaw %JVM_OPTS% -jar %AppName%

echo  starting鈥︹€?
echo  Start %AppName% success...
goto:eof

rem 鍑芥暟stop閫氳繃jps鍛戒护鏌ユ壘pid骞剁粨鏉熻繘绋?
:stop
	for /f "usebackq tokens=1-2" %%a in (`jps -l ^| findstr %AppName%`) do (
		set pid=%%a
		set image_name=%%b
	)
	if not defined pid (echo process %AppName% does not exists) else (
		echo prepare to kill %image_name%
		echo start kill %pid% ...
		rem 鏍规嵁杩涚▼ID锛宬ill杩涚▼
		taskkill /f /pid %pid%
	)
goto:eof
:restart
	call :stop
    call :start
goto:eof
:status
	for /f "usebackq tokens=1-2" %%a in (`jps -l ^| findstr %AppName%`) do (
		set pid=%%a
		set image_name=%%b
	)
	if not defined pid (echo process %AppName% is dead ) else (
		echo %image_name% is running
	)
goto:eof
