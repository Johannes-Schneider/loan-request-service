# Loan Request Service

The Loan Request Service is a simple Spring Boot application that provides RESTful APIs for managing loan requests.
More precisely, it offers the following features:

1. Creating new loan requests
2. Retrieving the sum of loan requests for a given customer

The full API specification is available [here](loan-request-service.yaml).

## Getting Started

### Prerequisites

- Java 17
- _Optional:_
    - [Maven 3.9](https://maven.apache.org/guides/getting-started/) (use the contained `mvnw` script if you don't have
      Maven installed)
    - [Docker](https://docs.docker.com/get-started/)
      and [Docker Compose](https://docs.docker.com/compose/gettingstarted/) (used for convenience deployment)

### Local Development

The Loan Request Service provides support for two different databases: [H2](https://www.h2database.com/html/main.html)
and [PostgreSQL](https://www.postgresql.org/).
Which database to use is configured via the active Spring profile; Either `h2` or `postgres`.
In addition to the Spring profiles, the project also comes with the same two profiles for Maven.
Using any of these Maven profiles will automatically set the corresponding Spring profile.
By default, the `h2` Maven (and, therefore, also Spring) profile is active.

Thus, running the application locally can be done as follows:

```shell
./mvnw spring-boot:run -P h2
```

(**Note**: Using the `-P h2` option is optional, as it is the default profile.)

### Deployment

The Loan Request Service can be deployed easily using Docker Compose:

```shell
docker compose up
```

This command will go through the [`docker-compose.yaml`](docker-compose.yaml) file, which defines two services:

1. The `loan-request-service` and
2. a `postgres` database.

Hereby, our application will be automatically built as defined in the [`Dockerfile`](Dockerfile).
Note that the `postgres` Maven profile will be used, assuming that the assembled docker image will be used in a
production environment.

#### Warning: Default Credentials

For the sake of convenience, the `postgres` service uses hard-coded credentials.
These credentials **are not secure** and should be changed in a production environment.
Furthermore, the same credentials are also hard-coded in the [`application.yaml`](src/main/resources/application.yaml)
file.
