#!/usr/bin/env bash
cd ..
mvn clean package
cp -fv target/mysqlreplicator-*.jar ./dist/
