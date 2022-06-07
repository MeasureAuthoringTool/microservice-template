# MADiE Measure Service

This is a Spring Boot micro-service which is responsible for operations associated with the MADiE Measure Micro Frontend.

# Running Locally

The application can be run as a spring-boot application. It requires a locally running MongoDb instance.

The MongoDb instance can be run with 

```
docker compose down --remove-orphans && docker volume prune && docker compose build --no-cache && docker compose up --force-recreate --build madie-mongo
```

The application can also be run in a Docker container.  This can be done by running

```
docker compose down --remove-orphans && docker volume prune && docker compose build --no-cache && docker compose up --force-recreate --build madie-measure
```

When the application runs with a spring.profile=IT it will use the madie-mongo service running in the container.  Running the application with no profile will allow the configuration that contains environment variables to be passed in.  This is how the ENTRY is configured in the dev-madie.hcqis.org instance that allows the application to run

#Testing
The application can be check for "health" by running Spring actuator endpoints.  For example, 

```
curl --location --request GET 'http://localhost:8080/api/actuator/health'
```

#Coverage

