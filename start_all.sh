#!/bin/bash

# Ensure log directory exists
mkdir -p /var/log

# 1. Setup Nginx Configuration
PORT=${PORT:-80}
echo "Configuring Nginx to listen on public port ${PORT}..."
sed "s/PORT_NUMBER/${PORT}/g" /app/nginx.conf.template > /etc/nginx/nginx.conf

echo "Starting Nginx..."
nginx

# 2. Setup and Start Kafka in KRaft mode (No Zookeeper)
echo "Formatting Kafka log directories..."
/opt/kafka/bin/kafka-storage.sh format -t 4L62xAE-Td61chw27IL65g -c /opt/kafka/config/kraft/server.properties

echo "Starting Kafka broker..."
export KAFKA_HEAP_OPTS="-Xmx64m -Xms64m"
/opt/kafka/bin/kafka-server-start.sh /opt/kafka/config/kraft/server.properties > /var/log/kafka.log 2>&1 &

# Wait for Kafka to initialize
echo "Waiting for Kafka to start..."
sleep 8

# 3. Define JVM Memory optimization arguments
# We use serial GC, tiered compilation (C1 compiler only), minimal thread stacks, metaspace limit,
# lazy initialization, and serialized startup order to fit into Render's 512MB RAM free tier.
JVM_OPTS="-Xmx24m -Xms16m -XX:MaxMetaspaceSize=32m -XX:ReservedCodeCacheSize=16m -Xss256k -XX:CICompilerCount=2 -XX:+UseSerialGC -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Dspring.main.lazy-initialization=true -Dspring.devtools.restart.enabled=false"

# Clear SERVER_PORT environment variable so it doesn't cause conflicts
unset SERVER_PORT
export EUREKA_CLIENT_SERVICEURL_DEFAULTZONE="http://localhost:9900/eureka/"
export SPRING_KAFKA_BOOTSTRAP_SERVERS="localhost:9092"

# 4. Start Discovery Server (Eureka)
echo "Starting discovery-server on port 9900..."
java $JVM_OPTS -jar /app/discovery-server.jar --server.port=9900 > /var/log/discovery-server.log 2>&1 &

# Wait for Eureka to be fully up before starting API Gateway and other services
echo "Waiting for Eureka discovery-server to initialize..."
sleep 20

# 5. Start API Gateway
echo "Starting api-gateway on port 9901..."
java $JVM_OPTS -jar /app/api-gateway.jar --server.port=9901 > /var/log/api-gateway.log 2>&1 &
sleep 6

# 6. Start the other microservices sequentially to spread CPU load during startup
services=(
    "user-service:9902"
    "product-service:9903"
    "cart-service:9904"
    "order-service:9905"
    "payment-service:9906"
    "wishlist-service:9907"
    "review-service:9908"
    "return-service:9909"
    "notification-service:9910"
)

for s in "${services[@]}"; do
    IFS=":" read -r name port <<< "$s"
    echo "Starting $name on port $port..."
    java $JVM_OPTS -jar /app/${name}.jar --server.port=${port} > /var/log/${name}.log 2>&1 &
    sleep 5
done

echo "EntityKart microservices are running!"
echo "Monitoring logs..."

# Keep container alive by tailing logs
tail -f /var/log/discovery-server.log /var/log/api-gateway.log /var/log/user-service.log
