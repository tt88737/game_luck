# 璐濆皵瀹為獙瀹?Spring 瀹樻柟鎺ㄨ崘闀滃儚 JDK涓嬭浇鍦板潃 https://bell-sw.com/pages/downloads/
FROM bellsoft/liberica-openjdk-rocky:17.0.16-cds
#FROM bellsoft/liberica-openjdk-rocky:21.0.8-cds
#FROM findepi/graalvm:java17-native

LABEL maintainer="Lion Li"

RUN mkdir -p /gameluck/server/logs \
    /gameluck/server/temp \
    /gameluck/skywalking/agent

WORKDIR /gameluck/server

ENV SERVER_PORT=8080 SNAIL_PORT=28080 LANG=C.UTF-8 LC_ALL=C.UTF-8 JAVA_OPTS=""

EXPOSE ${SERVER_PORT}
# 鏆撮湶 snail job 瀹㈡埛绔鍙?鐢ㄤ簬瀹氭椂浠诲姟璋冨害涓績閫氫俊
EXPOSE ${SNAIL_PORT}

ADD ./target/gameluck-admin.jar ./app.jar

SHELL ["/bin/bash", "-c"]

ENTRYPOINT java -Djava.security.egd=file:/dev/./urandom -Dserver.port=${SERVER_PORT} \
           -Dsnail-job.port=${SNAIL_PORT} \
           # 搴旂敤鍚嶇О 濡傛灉鎯冲尯鍒嗛泦缇よ妭鐐圭洃鎺?鏀规垚涓嶅悓鐨勫悕绉板嵆鍙?
           #-Dskywalking.agent.service_name=gameluck-server \
           #-javaagent:/gameluck/skywalking/agent/skywalking-agent.jar \
           -XX:+HeapDumpOnOutOfMemoryError -XX:+UseZGC ${JAVA_OPTS} \
           -jar app.jar

