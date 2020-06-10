# Kumoru

image:https://img.shields.io/badge/vert.x-3.9.0-purple.svg[link="https://vertx.io"]

Kumoru is a minimal, lightweight alternative to Sonatype Nexus.

## Features

1. Mirrors the following repositories only. The repositories are accessed in the given order.
    * [x] Maven 2.
    * [x] JCenter.
    * [x] Jitpack.
2. Snapshots repositories of above mirrors are enabled.
3. Snapshots are checked for updates every 12 hours.
4. You can 'Publish' artifacts to Kumoru.

## Running the server

To run your application:
```
docker run -p 8888:8888 -v /tmp/repo:/srv/repo rishabh9/kumoru
```

> `/tmp/repo` should be replaced with a path of your choice.
> This is the location where the artifacts will be downloaded on the host machine.

To enable access logs, set the environment variable `KUMORU_ACCESS_LOG=true` as:
```
docker run -e KUMORU_ACCESS_LOG=true -p 8888:8888 -v /tmp/repo:/srv/repo rishabh9/kumoru
```

> All logs are written to the `STDOUT`.

To run the container on a different time zone (default is GMT), set the environment variable `TZ`. Example:
```
docker run -p 8888:8888 -v /tmp/repo:/srv/repo -e TZ=Europe/Amsterdam rishabh9/kumoru
```

## Configuring Maven

1. Mirroring

    Edit Maven's `settings.xml` and add a `<mirror>`:
    
    ```
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
    
    ```
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
    
    ```
    repositories {
      maven {
        url: "http://localhost:8888"
      }
    }
    ```
2. Publishing

    Add in your `build.gradle`
    
    ```
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
```
./gradlew clean build
```

To package Kumoru as Docker container:
```
./gradlew jibDockerBuild
```
