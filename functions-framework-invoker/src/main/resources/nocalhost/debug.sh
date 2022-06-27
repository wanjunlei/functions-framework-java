#!/bin/bash

mvn clean compile dependency:copy-dependencies
mvn exec:exec -Dexec.executable="java" -Dexec.args="-classpath target/classes/:target/dependency/* -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005 dev.openfunction.invoker.Runner"
