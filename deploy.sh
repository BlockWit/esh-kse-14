#!/bin/bash

PROJECT_NAME=esh-kse-14
mvn install
sudo cp target/${PROJECT_NAME}-1.0-SNAPSHOT-jar-with-dependencies.jar /opt/kafka/libs/esh-kse-example.jar
sudo chown kafka:kafka /opt/kafka/libs/esh-kse-example.jar
sudo cp src/test/resources/server.properties /opt/kafka/config/server.properties
sudo systemctl restart kafka
