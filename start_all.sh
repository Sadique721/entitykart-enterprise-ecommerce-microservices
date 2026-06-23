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
sleep 15

# 3. Define JVM Memory optimization arguments for Render deployment
# We use serial GC, tiered compilation (C1 compiler only), minimal thread stacks, metaspace limit (75m),
# lazy initialization, and serialized startup order to fit into Render's 512MB RAM free tier.
JVM_OPTS="-Xmx32m -Xms20m -XX:MaxMetaspaceSize=75m -XX:ReservedCodeCacheSize=12m -Xss192k -XX:CICompilerCount=1 -XX:+UseSerialGC -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Djava.security.egd=file:/dev/./urandom -Dspring.main.lazy-initialization=true -Dspring.devtools.restart.enabled=false"

# Clear SERVER_PORT environment variable so it doesn't cause conflicts
unset SERVER_PORT

# Export default configuration variables for discovery and Kafka
export EUREKA_CLIENT_SERVICEURL_DEFAULTZONE="http://localhost:9900/eureka/"
export SPRING_KAFKA_BOOTSTRAP_SERVERS="localhost:9092"

# 4. Start Group 1 (Infrastructure & Priority Services)
echo "Starting discovery-server on port 9900..."
java $JVM_OPTS -jar /app/discovery-server.jar --server.port=9900 > /var/log/discovery-server.log 2>&1 &

echo "Waiting for Eureka discovery-server to initialize..."
sleep 25

echo "Starting api-gateway on port 9901..."
java $JVM_OPTS -jar /app/api-gateway.jar --server.port=9901 > /var/log/api-gateway.log 2>&1 &

HIKARI_OPTS="--spring.datasource.hikari.maximum-pool-size=2 --spring.datasource.hikari.minimum-idle=1"

echo "Starting notification-service on port 9910..."
DB_NAME=notification_service java $JVM_OPTS -jar /app/notification-service.jar --server.port=9910 $HIKARI_OPTS > /var/log/notification-service.log 2>&1 &

echo "Waiting for Group 1 services to initialize..."
sleep 20

# 5. Start Group 2 (Core Business Microservices) sequentially
services=(
    "user-service:9902"
    "product-service:9903"
    "cart-service:9904"
    "order-service:9905"
    "payment-service:9906"
    "wishlist-service:9907"
    "review-service:9908"
    "return-service:9909"
)

for s in "${services[@]}"; do
    IFS=":" read -r name port <<< "$s"
    db_name=$(echo "$name" | tr '-' '_')
    echo "Starting $name on port $port with database $db_name..."
    DB_NAME=$db_name java $JVM_OPTS -jar /app/${name}.jar --server.port=${port} $HIKARI_OPTS > /var/log/${name}.log 2>&1 &
    sleep 20
done

echo "EntityKart microservices are running!"
echo "Monitoring logs..."

# Keep container alive by tailing logs
tail -f /var/log/discovery-server.log /var/log/api-gateway.log /var/log/user-service.log /var/log/product-service.log
