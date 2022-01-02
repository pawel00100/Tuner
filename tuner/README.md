##Run locally

Adjust application.properties, then:

```
mvn install
java -jar .\target\tuner-<version>.jar
```

##Docker image creation and deployment

Create jar

```
mvn install
```

Create docker image in tuner directory. For x86 platform does not needed to be specified, other platforms may need adjustment.

```
docker build --platform=linux/arm/v7 --tag=<tag> .
```

Run docker image. After image tag you can add flags overriding properties file.

```
docker run -d \
-p <dev api port>:8080 -p <debug port>:5005 \
-v <directory for recorded files>:/opt/recordings \
--name <image name> <tag>  \
--server.url=http://<server ip>:<server port> \
--tvheadened.url=http://<tvh ip>:<tvh port> \
--recording.location=/opt/recordings
```

Optional flags for converting recordings.

```
--converter.enabled=true \
--converter.converted.location=<directory for recorded files> \
--converter.ffmpeg.exec=ffmpeg \
--converter.codec.video=libx264 \
```