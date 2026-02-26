FROM eclipse-temurin:25-jdk

WORKDIR /workspace

COPY exam/ .

RUN chmod +x ./gradlew && \
    # testClasses 比 dependencies 更完整：下載 JAR + 編譯 main/test 程式碼，確保快取完全
    ./gradlew testClasses --no-daemon

CMD ["tail", "-f", "/dev/null"]
