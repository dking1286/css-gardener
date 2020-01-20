# Docker image for running the continuous integration pipeline on Circleci
FROM theasp/clojurescript-nodejs:latest

ENV CLOJURE_VERSION=1.10.1.483
ENV CLOJURE_INSTALLER=linux-install-$CLOJURE_VERSION.sh

WORKDIR /tmp

# Install Clojure CLI
# Adapted from https://github.com/Quantisan/docker-clojure/blob/be736864bf3e6d35c1bc6b13fd2add4a319fb92b/target/openjdk-14-alpine/tools-deps/Dockerfile
RUN  wget https://download.clojure.org/install/$CLOJURE_INSTALLER && \
        chmod +x $CLOJURE_INSTALLER && \
        ./$CLOJURE_INSTALLER && \
        clojure -e "(clojure-version)"
