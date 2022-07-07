# FastDiagPEvaluation_copy

This repository provides two implementations of FastDiagP, i.e., **FastDiagV4** and **FastDiagV6**.

## Table of Contents

- [Repository structure](#repository-structure)
- [How to run the source code](#how-to-run-the-source-code)
- [CCManager](#ccmanager)
- [FastDiagV6](#fastdiagv6)
- [FastDiagV4](#fastdiagv4)

## Repository structure

| *folder*                 | *description*                                                              |
|--------------------------|----------------------------------------------------------------------------|
| ./data/kb                | stores the *Linux-2.6.33.3* feature model, which is used in some unit tests |
| ./src/main/              | source code                                                                |
| ./src/test/.../fastdiagp | provides two unit tests for two algorithms **FastDiagPV4** and **FastDiagPV6**     |
| ./src/test/.../linux     | provides 5 tests on the basis of the *Linux-2.6.33.3* feature model    |

## How to run the source code

> **JDK requirement:** Eclipse Temurin 17.0.2

### Get the Maven dependencies from the GitHub package repository

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

### Tests

You could find two **unit tests** for two algorithms **FastDiagPV4** and **FastDiagPV6** in the *./src/test/.../fastdiagp* folder.

Besides, there are five tests in the *./src/test/.../linux* folder, which use the *Linux-2.6.33.3* feature model as the knowledge base.
Each test compares the runtime of three algorithms **FastDiag**, **FastDiagPV4** and **FastDiagPV6** with different cardinalities of 
user requirements. For example, _LinuxTest8_ is the test of a user requirement with the cardinality of 8.

## CCManager

_ChocoConsistencyChecker_ is a wrapper of the **Choco Solver**. It provides a set of methods to check the consistency 
of a constraint set. However, since **Choco Solver** is not _thread-safe_, we need to create a new _ChocoConsistencyChecker_
for each thread.

_CCManager_ manages a list of pre-generated _ChocoConsistencyChecker_. In the parallel scheme, when a worker need 
to check the consistency of a constraint set, it will ask _CCManager_ to get a free _ChocoConsistencyChecker_ that 
is not being used by other workers.

## FastDiagV6

This is an implementation of the algorithm presented in the paper. The implementation applies _LookupTableV6_ 
to store results of the predicted consistency checks.

_LookupTableV6_ encapsulates a _ConcurrentMap_, in which each item is a pair of a hash code (a hash code of a constraint set) 
and an instance of _ConsistencyCheckResultV6_.

There is a semaphore in _LookupTableV6_. However, it is only used when clearing the Lookup table 
or printing the Lookup table into the log (the Console or a file).

Similar to **FastDiag**, **FastDiagV6** has two functions _findDiagnosis_ and _fd_ implementing two algorithms of **FastDiag**, 
i.e., _FastDiag_ and _FD_. There are two following different points:

1. In the _findDiagnosis_ functions, two ForkJoin pools are created. The first pool (_lookaheadPool_) is used 
to run the _lookAhead_ function. The second pool (_checkerPool_) is used to run consistency check workers.
2. The _isConsistent_ function - The original **FastDiag** call directly the _isConsistent_ functions of the _ChocoConsistencyChecker_.
In contrast, **FastDiagV6** has its own _isConsistent_ function. It first checks the existence of the consistency check for B U C.
If this is not the case, it will activate a **LookAheadWorkerV6** on the _lookAheadPool_ to predict 
and generate further potential consistency checks. In the meantime, it executes the consistency check for B U C
on the **main thread**. Otherwise, i.e., if the consistency check for B U C is already generated, 
it will directly return the result (using the _getConsistency_ function of **LookupTableV6**).

## FastDiagV4

Since the runtime of consistency check (on the **main thread**) is really fast, consistency checks on other threads needs to be
faster or the LookAhead must look ahead further. In line with this idea, this implementation uses a **LookAheadWorkerV4** to predict
and generate potential consistency checks as follows:

1. Only generate and activate consistency checks generated by the _consistent_ assumption of B U C 
(the right branches of the **LookAhead** execution trace). 
2. The _lookAhead_ function of **LookAheadWorkerV4** returns a list of potential consistency checks.
The compute function of **LookAheadWorkerV4** takes care of filtering and activating the potential consistency checks.
3. The _lookAhead_ function executes the _consistent_ assumption first, and then the _inconsistent_ assumption.

**FastDiagV4** applies _LookupTableV4_ to store results of the predicted consistency checks. _LookupTableV4_ is basically
the same as _LookupTableV6_, except that it stores _LookAheadNodeV4_ instead of _ConsistencyCheckResultV6_.