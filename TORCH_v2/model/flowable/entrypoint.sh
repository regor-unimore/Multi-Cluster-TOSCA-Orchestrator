#!/bin/sh

# 1) copy custom jars to classpath folder
echo looking for custom jar to deploy in the classpath...
cp /custom-jars/*.jar /app/WEB-INF/lib/
echo Custom Jars deployed in the classpath
exec "$@"
