#!/usr/bin/env bash
java -XX:+UseConcMarkSweepGC \
	 -XX:+CMSParallelRemarkEnabled \
	 -XX:+UseCMSInitiatingOccupancyOnly \
	 -XX:+ScavengeBeforeFullGC \
	 -XX:+CMSScavengeBeforeRemark \
	 -jar mysqlreplicator-1.0.0.jar \
	 --spring.config.location=file:./application.properties \
	 --logging.config=file:./logback.xml
