## Use AWS AL2 + Corretto base image
FROM cgr.dev/chainguard/jdk:latest-dev

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

USER root
RUN apk add coreutils curl

#Update Packages
#RUN yum update -y --security

## Add the wait script to the image
COPY --from=ghcr.io/ufoscout/docker-compose-wait:latest /wait /wait

## Download new relic java agent
RUN curl -O https://download.newrelic.com/newrelic/java-agent/newrelic-agent/current/newrelic.jar \
    && curl -O https://download.newrelic.com/newrelic/java-agent/newrelic-agent/current/newrelic.yml

## Launch the wait tool and then your application
ENTRYPOINT ["java","-jar","-Dspring.profiles.active=it", "app.jar"]
