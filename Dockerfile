FROM openjdk:16-jdk
RUN mkdir /app /data
COPY ./build/install/lisu/ /app/

WORKDIR /app/bin
VOLUME /data
EXPOSE 8080:8080
CMD ["./lisu", "/data"]