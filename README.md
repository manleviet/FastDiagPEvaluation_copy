# FastDiagPEvaluation_copy

This repository provides two implementations of FastDiagP.

## Table of Contents

- [Repository structure](#repository-structure)
- [Evaluation process](#evaluation-process)
- [How to reproduce the experiment](#how-to-reproduce-the-experiment)
  - [Build a Docker image](#build-a-docker-image)
  - [Use pre-build Java applications](#use-pre-build-java-applications)
  - [Build apps by yourself](#build-apps-by-yourself)

## Repository structure

| *folder*         | *description*                                                                           |
|------------------|-----------------------------------------------------------------------------------------|
| ./data/kb        | stores the *Linux-2.6.33.3* feature model, which is used in some unit tests |
| ./data           | s, a test suite, and scenarios                     |
| ./data/testsuite | stores a test suite of the original *Linux-2.6.33.3* feature model                      |
| ./data/scenarios | contains scenarios selected to evaluate the **WipeOutR_T** algorithm                    |
| ./data/results   | evaluation results published in the paper                                               |
| ./docs           | guides of *jar* files                                                                   |
| ./results        | stores the results                                                       |
| ./src            | source code                                                                             |
| ./shell          | bash scripts to execute the evaluations                                                 |
| ./docker         | a bash script and a copy of configuration files, which are used to build a Docker image |
| Dockerfile       | Dockerfile to build the Docker image                                                    |
| settings.xml     | settings of the GitHub Maven repository                                                 |
 
> **JDK requirement:** Eclipse Temurin 17.0.2

#### Get the Maven dependencies from the GitHub package repository

Our implementation depends on our [CA-CDR library](https://github.com/manleviet/CA-CDR-V2). Thus, after cloning the source code into your system,
you need to add the below script in your *settings.xml* file (*not the settings.xml attached in the repository*) to download the dependencies from the GitHub package repository.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
    <activeProfiles>
        <activeProfile>github</activeProfile>
    </activeProfiles>

    <profiles>
        <profile>
            <id>github</id>
            <repositories>
                <repository>
                    <id>central</id>
                    <url>https://repo1.maven.org/maven2</url>
                </repository>
                <repository>
                    <id>github</id>
                    <url>https://maven.pkg.github.com/manleviet/*</url>
                </repository>
            </repositories>
        </profile>
    </profiles>
    
    <servers>
        <server>
            <id>github</id>
            <username>USERNAME</username>
            <password>TOKEN</password>
        </server>
    </servers>
</settings>
```
Replacing USERNAME with your GitHub username, and TOKEN with your personal access token 
(see [Creating a personal access token](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/creating-a-personal-access-token)). Note: your token must have the ```read:packages``` scope.
