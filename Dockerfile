FROM eclipse-temurin:17-jre-alpine

RUN mkdir /app
WORKDIR /app

COPY build/libs/*-all.jar /app/lobby.jar
COPY run/lobby.tnt /app/lobby.tnt

CMD ["java", "-jar", "/app/lobby.jar"]