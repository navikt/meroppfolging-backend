FROM ghcr.io/navikt/baseimages/temurin:21

ENV JDK_JAVA_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+UseParallelGC -XX:ActiveProcessorCount=2"

LABEL org.opencontainers.image.source=https://github.com/navikt/meroppfolging-backend
COPY build/libs/*.jar app.jar
