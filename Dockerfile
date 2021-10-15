FROM openjdk:16-jdk AS builder
COPY . /tmp/lisu
WORKDIR /tmp/lisu
RUN ./gradlew installDist

FROM openjdk:16-jdk
COPY --from=builder /tmp/lisu/build/install/lisu /app
WORKDIR /app/bin
VOLUME /data
EXPOSE 8080
CMD ["./lisu", "/data"]