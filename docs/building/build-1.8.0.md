# Building 1.8.0

This tutorial explains how to build release [1.8.0](https://github.com/JetBrains/kotlin/releases/tag/v1.8.0) locally.

## Prerequisites

You must have:
* Linux or MacOS.
* Docker installed.
* 14 GB memory available and configured in Docker.

## Set environment variables

The following environment variables must be set:

```
export DEPLOY_VERSION=1.8.0
export BUILD_NUMBER=1.8.0-release-345
export KOTLIN_NATIVE_VERSION=1.8.0
export DOCKER_CONTAINER_URL=kotlin.registry.jetbrains.space/p/kotlin/containers/kotlin-build-env:v6
```

## Clone Kotlin repository

In a new folder, clone the release tag from the Kotlin repository, and change directory to the build folder:

```
git clone --depth 1 --branch v$DEPLOY_VERSION https://github.com/JetBrains/kotlin.git kotlin-build-$DEPLOY_VERSION
cd kotlin-build-$DEPLOY_VERSION
```

## Build and verify the compiler

```
docker run --rm -it --name kotlin-build-$DEPLOY_VERSION \
  --workdir="/repo" --volume="$(pwd):/repo" $DOCKER_CONTAINER_URL \
  /bin/bash -c "./scripts/build-kotlin-compiler.sh $DEPLOY_VERSION $BUILD_NUMBER"
```

This generates a ZIP file: `dist/kotlin-compiler-1.8.0.zip`.

Check that the SHA 256 checksum is equal to the published one for [kotlin-compiler-1.8.0.zip](https://github.com/JetBrains/kotlin/releases/download/v1.8.0/kotlin-compiler-1.8.0.zip):

0bb9419fac9832a56a3a19cad282f8f2d6f1237d2d467dc8dfe9bd4a2a43c42e

## Build and verify maven artifacts

```
export BUILD_NUMBER=1.8.0-release-345(1.8.0)
docker run --rm -it --name kotlin-build-$DEPLOY_VERSION \
  --workdir="/repo" --volume="$(pwd):/repo" $DOCKER_CONTAINER_URL \
  /bin/bash -c "./scripts/build-kotlin-maven.sh $DEPLOY_VERSION '$BUILD_NUMBER' $KOTLIN_NATIVE_VERSION"
docker run --rm -it --name kotlin-build-repack-zip-with-stable-entries-order-$DEPLOY_VERSION \
  --workdir="/repo" --volume="$(pwd):/repo" $DOCKER_CONTAINER_URL \
  /bin/bash -c "cd build/repo-reproducible && rm reproducible-maven-$DEPLOY_VERSION.zip || true && find . -type f | sort | zip -X reproducible-maven-$DEPLOY_VERSION.zip -@"
```

This generates a ZIP file: `build/repo-reproducible/reproducible-maven-1.8.0.zip`.

Check that the SHA 256 checksum is equal to the published one for [maven-1.8.0.zip](https://github.com/JetBrains/kotlin/releases/download/v1.8.0/maven-1.8.0.zip):

98573938b708c193ca68c7269d29e6ac777dfe780b3935bc733d4c428a45d4e5