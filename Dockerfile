FROM openjdk:8-jdk-alpine
VOLUME /tmp
# ADD build/libs/jenagraph-0.2.0.jar app.jar
COPY output/jenagraph-0.2.0.jar app.jar
ENV JAVA_OPTS=""
# ENTRYPOINT exec java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app.jar
ENTRYPOINT exec java -jar /app.jar
