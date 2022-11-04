FROM gradle:7-jdk18 as builder

COPY . /lisu
WORKDIR /lisu

RUN gradle --no-daemon installDist

RUN jdeps --print-module-deps \
    --ignore-missing-deps \
    --recursive \
    --multi-release 17 \
    --class-path="./build/install/lisu/lib/*" \
    --module-path="./build/install/lisu/lib/*" \
    ./build/install/lisu/lib/lisu*.jar > jre-deps.info

RUN jlink --verbose \
    --compress 2 \
    --strip-debug \
    --no-header-files \
    --no-man-pages \
    --output /javaruntime \
    --add-modules $(cat jre-deps.info)


FROM debian:buster-slim

ENV JAVA_HOME=/opt/java/openjdk
ENV PATH "${JAVA_HOME}/bin:${PATH}"

COPY --from=builder /javaruntime $JAVA_HOME
COPY --from=builder /lisu/build/install/lisu /lisu

ENV LANG C.UTF-8

VOLUME /mangas

EXPOSE 8080

CMD ["./lisu/bin/lisu", "/mangas"]