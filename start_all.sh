#!/bin/bash

# Ensure log directory exists
mkdir -p /var/log

# 1. Setup Nginx Configuration
PORT=${PORT:-80}
echo "Configuring Nginx to listen on public port ${PORT}..."
sed "s/PORT_NUMBER/${PORT}/g" /app/nginx.conf.template > /etc/nginx/nginx.conf

echo "Starting Nginx..."
nginx

# 2. Skip Kafka to conserve memory on Render 512MB free tier
echo "Skipping Kafka startup to save memory..."

# 3. Define JVM Memory optimization arguments
# We use serial GC, tiered compilation (C1 compiler only), minimal thread stacks, metaspace limit,
# lazy initialization, and serialized startup order to fit into Render's 512MB RAM free tier.
JVM_OPTS="-Xmx32m -Xms20m -XX:MaxMetaspaceSize=36m -XX:ReservedCodeCacheSize=12m -Xss192k -XX:CICompilerCount=1 -XX:+UseSerialGC -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Djava.security.egd=file:/dev/./urandom -Dspring.main.lazy-initialization=true -Dspring.devtools.restart.enabled=false"

# Clear SERVER_PORT environment variable so it doesn't cause conflicts
unset SERVER_PORT
export EUREKA_CLIENT_SERVICEURL_DEFAULTZONE="http://localhost:9900/eureka/"

# 4. Start Discovery Server (Eureka)
echo "Starting discovery-server on port 9900..."
java $JVM_OPTS -jar /app/discovery-server.jar --server.port=9900 > /var/log/discovery-server.log 2>&1 &

# Wait for Eureka to be fully up before starting API Gateway and other services
echo "Waiting for Eureka discovery-server to initialize..."
sleep 25

# 5. Start API Gateway
echo "Starting api-gateway on port 9901..."
java $JVM_OPTS -jar /app/api-gateway.jar --server.port=9901 > /var/log/api-gateway.log 2>&1 &
sleep 15

# 6. Start core microservices (user-service and product-service) sequentially
services=(
    "user-service:9902"
    "product-service:9903"
)

HIKARI_OPTS="--spring.datasource.hikari.maximum-pool-size=2 --spring.datasource.hikari.minimum-idle=1"

for s in "${services[@]}"; do
    IFS=":" read -r name port <<< "$s"
    echo "Starting $name on port $port..."
    java $JVM_OPTS -jar /app/${name}.jar --server.port=${port} $HIKARI_OPTS > /var/log/${name}.log 2>&1 &
    sleep 20
done

echo "EntityKart microservices are running!"
echo "Monitoring logs..."

# Keep container alive by tailing logs
tail -f /var/log/discovery-server.log /var/log/api-gateway.log /var/log/user-service.log
