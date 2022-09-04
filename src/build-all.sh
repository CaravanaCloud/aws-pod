#!/bin/bash
export QUARKUS_PROFILE="prod"
 mvn -f demo-multiverse/pom.xml clean install
 mvn -f demo-multiverse-cdk/pom.xml clean install
 mvn -f demo-multiverse-sls/pom.xml clean install