FROM gradle:latest as builder
COPY --chown=gradle:gradle ./PForward /home/gradle/project
WORKDIR /home/gradle/project
RUN gradle build -DsocksProxyHost=127.0.0.1 -DsocksProxyPort=9050 --warning-mode all

FROM openjdk:11-jre-slim
COPY --from=builder /home/gradle/project/build/libs/PForward.jar /app/PForward.jar
WORKDIR /app
CMD java -jar PForward.jar
