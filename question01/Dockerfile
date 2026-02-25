FROM eclipse-temurin:25-jdk

WORKDIR /workspace

COPY exam/ .

RUN chmod +x ./gradlew && \
    ./gradlew dependencies --no-daemon

CMD ["tail", "-f", "/dev/null"]
