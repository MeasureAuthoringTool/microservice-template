## Use whatever base image
FROM adoptopenjdk/openjdk16:jre-16_36

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

## Add the wait script to the image
ADD https://github.com/ufoscout/docker-compose-wait/releases/download/2.7.3/wait /wait
RUN chmod +x /wait

## Launch the wait tool and then your application
ENTRYPOINT ["java","-jar","-Dspring.profiles.active=it", "app.jar"]