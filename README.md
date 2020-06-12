# Kumoru

Kumoru is a minimal, lightweight Maven style artifacts repository manager. I wanted an alternative to Sonatype Nexus,
and hence, Kumoru.

## Features

Most of the projects use the following repositories. Hence, only these are proxied by default.
1. Maven 2.
2. JCenter.
3. Jitpack.

A lookup for an artifact is performed in the same order as they are listed above.

Snapshots repositories of above mirrors are also enabled. Hence, snapshots can be proxied too.

An update for a snapshot is checked every 12 hours (since server restart).

You can 'Publish' artifacts to Kumoru. Tested with Gradle's publish plugin. Should work with Maven publish too.

## Caveats

1. There is no way to 'browse' artifacts proxied by the server, from the browser. You'll need to access the Docker volume directly.
2. There is no authentication of any form. Proxy Kumoru behind a proxy server that provides authentication.

## Running the server

To run your application:
```shell script
docker run -p 8888:8888 -v /tmp/repo:/srv/repo rishabh9/kumoru
```

> `/tmp/repo` should be replaced with a path of your choice.
> This is the location where the artifacts will be downloaded on the host machine.

To enable access logs, set the environment variable `KUMORU_ACCESS_LOG=true` as:
```shell script
docker run -e KUMORU_ACCESS_LOG=true -p 8888:8888 -v /tmp/repo:/srv/repo rishabh9/kumoru
```

> All logs are written to the `STDOUT`.

To run the container on a different time zone (default is GMT), set the environment variable `TZ`. Example:
```shell script
docker run -p 8888:8888 -v /tmp/repo:/srv/repo -e TZ=Europe/Amsterdam rishabh9/kumoru
```

### Specifying repositories

Create a file `repositories.json`. For sample, see the default file under the folder `src/main/resources`.
Modify it to add/remove repositories you want.

To configure a repository that requires basic authentication, configure as:

```json
    {
      "name": "DemoRepoBasicAuth",
      "url": "http://maven-repo.demo:8080",
      "basicAuth": {
        "username": "demo",
        "password": "demo"
      }
    }
```

To configure a repository that requires bearer token for authentication, configure as:

```json
    {
      "name": "DemoRepoBearerToken",
      "url": "http://maven-repo.demo:8080",
      "bearerToken": {
        "token": "demoToken"
      }
    }
```

Use your custom `repositories.json` file as:

```shell script
docker run -p 8888:8888 -v /tmp/repo:/srv/repo -v /path/to/custom/repositories.json:/app/resource/repositories.json rishabh9/kumoru
```

> `/path/to/custom/repositories.json` should be replaced with the path where you have placed your custom `repositories.json`.

## Configuring Maven

1. Mirroring

    Edit Maven's `settings.xml` and add a `<mirror>`:
    
    ```xml
      <mirrors>
        <mirror>
          <id>kumoru</id>
          <mirrorOf>*</mirrorOf>
          <name>Kumoru - A minimal Nexus repository</name>
          <url>http://localhost:8888</url>
        </mirror>
    
      </mirrors>
    ```
2. Publishing

    In your `pom.xml` add
    
    ```xml
      <distributionManagement>
        <repository>
          <id>kumoru</id>
          <name>Kumoru - A minimal Nexus repository</name>
          <url>http://localhost:8888</url>
        </repository>
      </distributionManagement>
    ```

## Configuring Gradle

1. Mirroring

    Add in your `build.gradle`
    
    ```groovy
    repositories {
      maven {
        url: "http://localhost:8888"
      }
    }
    ```
2. Publishing

    Add in your `build.gradle`
    
    ```groovy
    publishing {
      repositories {
        maven {
          url: "http://localhost:8888"
        }
      }
    }
    ```

## Building the code

### Pre-Requisites

1. JDK 11
2. Docker

### Build

To build Kumoru source:
```shell script
./gradlew clean build
```

To package Kumoru as Docker container:
```shell script
./gradlew jibDockerBuild
```
