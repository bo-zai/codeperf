FROM eclipse-temurin:8-jre

WORKDIR /opt/deployments

COPY ./target/codeperf-demo-app.jar /opt/deployments/

ENV JAVA_OPTS="-Xms2g -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:InitiatingHeapOccupancyPercent=45 -XX:+PrintGCDetails -XX:+PrintGCDateStamps"

EXPOSE 8080

CMD java ${JAVA_OPTS} -cp /opt/deployments/. org.springframework.boot.loader.JarLauncher


# CODEPERF_AGENT_START
COPY target/codeperf/ /opt/codeperf/
ENV JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS} -javaagent:/opt/codeperf/codeperf-agent.jar=/opt/codeperf/agent.yml"
# CODEPERF_AGENT_END
