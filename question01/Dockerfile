FROM eclipse-temurin:25-jdk

WORKDIR /tmp/workspace

COPY exam/ .

RUN chmod +x ./gradlew && \
    ./gradlew dependencies --no-daemon
