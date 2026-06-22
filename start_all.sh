#!/bin/bash

# Ensure log directory exists
mkdir -p /var/log

# 1. Setup Nginx Configuration
PORT=${PORT:-80}
echo "Configuring Nginx to listen on public port ${PORT}..."
sed "s/PORT_NUMBER/${PORT}/g" /app/nginx.conf.template > /etc/nginx/nginx.conf

echo "Starting Nginx..."
nginx

# 2. Skip Kafka and Eureka to conserve memory on Render 512MB free tier
echo "Skipping Kafka and Eureka startup to save memory..."

# 3. Define JVM Memory optimization arguments
# We use serial GC, tiered compilation (C1 compiler only), minimal thread stacks, metaspace limit (75m to avoid Metaspace OOM),
# lazy initialization, disabled discovery client, and serialized startup order to fit into Render's 512MB RAM free tier.
JVM_OPTS="-Xmx32m -Xms20m -XX:MaxMetaspaceSize=75m -XX:ReservedCodeCacheSize=12m -Xss192k -XX:CICompilerCount=1 -XX:+UseSerialGC -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Djava.security.egd=file:/dev/./urandom -Dspring.main.lazy-initialization=true -Dspring.devtools.restart.enabled=false -Dspring.cloud.discovery.enabled=false -Deureka.client.enabled=false"

# Clear SERVER_PORT environment variable so it doesn't cause conflicts
unset SERVER_PORT

# 4. Start API Gateway (direct routing to local ports, disabling Eureka client)
echo "Starting api-gateway on port 9901..."
java $JVM_OPTS -jar /app/api-gateway.jar \
  --server.port=9901 \
  --spring.cloud.gateway.routes[0].id=user-service \
  --spring.cloud.gateway.routes[0].uri=http://localhost:9902 \
  --spring.cloud.gateway.routes[0].predicates[0]=Path=/api/users/** \
  --spring.cloud.gateway.routes[1].id=product-service-products \
  --spring.cloud.gateway.routes[1].uri=http://localhost:9903 \
  --spring.cloud.gateway.routes[1].predicates[0]=Path=/api/products/** \
  --spring.cloud.gateway.routes[2].id=product-service-categories \
  --spring.cloud.gateway.routes[2].uri=http://localhost:9903 \
  --spring.cloud.gateway.routes[2].predicates[0]=Path=/api/categories/** \
  > /var/log/api-gateway.log 2>&1 &

# Wait for API Gateway to boot
echo "Waiting for api-gateway to initialize..."
sleep 20

# 5. Start core microservices (user-service and product-service) sequentially
services=(
    "user-service:9902"
    "product-service:9903"
)

HIKARI_OPTS="--spring.datasource.hikari.maximum-pool-size=2 --spring.datasource.hikari.minimum-idle=1"

for s in "${services[@]}"; do
    IFS=":" read -r name port <<< "$s"

    # Original startup (Commented on 2026-06-22):
    # echo "Starting $name on port $port..."
    # java $JVM_OPTS -jar /app/${name}.jar --server.port=${port} $HIKARI_OPTS > /var/log/${name}.log 2>&1 &

    # Updated 2026-06-22: Dynamic DB_NAME mapping to connect each microservice to its own schema on Aiven MySQL
    db_name=$(echo "$name" | tr '-' '_')
    echo "Starting $name on port $port with database $db_name..."
    DB_NAME=$db_name java $JVM_OPTS -jar /app/${name}.jar --server.port=${port} $HIKARI_OPTS > /var/log/${name}.log 2>&1 &
    sleep 20
done

echo "EntityKart microservices are running!"
echo "Monitoring logs..."

# Keep container alive by tailing logs
tail -f /var/log/api-gateway.log /var/log/user-service.log /var/log/product-service.log
