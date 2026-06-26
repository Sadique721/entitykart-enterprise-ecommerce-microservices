#!/bin/bash

# ─────────────────────────────────────────────────────────────────────────────
# EntityKart – Render Single-Container Startup Script
# Updated 2026-06-26: Replaced api-gateway + discovery-server + notification-service
#                      with merged common-services.jar (port 9900)
# ─────────────────────────────────────────────────────────────────────────────

mkdir -p /var/log

# Limit glibc memory arenas to reduce JVM memory fragmentation
export MALLOC_ARENA_MAX=1

# ── 1. Setup Nginx ────────────────────────────────────────────────────────────
PORT=${PORT:-80}
echo "Configuring Nginx to listen on public port ${PORT}..."
sed "s/PORT_NUMBER/${PORT}/g" /app/nginx.conf.template > /etc/nginx/nginx.conf

echo "Starting Nginx..."
nginx

# ── 2. Start Kafka in KRaft mode (no Zookeeper) ──────────────────────────────
echo "Formatting Kafka log directories..."
/opt/kafka/bin/kafka-storage.sh format -t 4L62xAE-Td61chw27IL65g -c /opt/kafka/config/kraft/server.properties

echo "Starting Kafka broker..."
export KAFKA_HEAP_OPTS="-Xmx32m -Xms32m"
/opt/kafka/bin/kafka-server-start.sh /opt/kafka/config/kraft/server.properties > /var/log/kafka.log 2>&1 &

echo "Waiting for Kafka to start (15s)..."
sleep 15

# ── 3. JVM Options (tuned for Render free-tier 512MB) ────────────────────────
JVM_OPTS="-Xmx48m -Xms24m -XX:MaxMetaspaceSize=72m -XX:ReservedCodeCacheSize=10m \
  -Xss160k -XX:CICompilerCount=1 -XX:+UseSerialGC -XX:+TieredCompilation \
  -XX:TieredStopAtLevel=1 -XX:+UseStringDeduplication \
  -Djava.security.egd=file:/dev/./urandom \
  -Dspring.main.lazy-initialization=true \
  -Dspring.devtools.restart.enabled=false"

unset SERVER_PORT
export EUREKA_CLIENT_SERVICEURL_DEFAULTZONE="http://localhost:9900/eureka/"
export SPRING_KAFKA_BOOTSTRAP_SERVERS="localhost:9092"

HIKARI_OPTS="--spring.datasource.hikari.maximum-pool-size=2 --spring.datasource.hikari.minimum-idle=1"

# ── 4. Start common-services (Eureka + Gateway + Notification) on port 9900 ──
echo "Starting common-services on port 9900 (Eureka + Gateway + Notifications)..."
DB_NAME=notification_service java $JVM_OPTS -jar /app/common-services.jar \
  --server.port=9900 $HIKARI_OPTS > /var/log/common-services.log 2>&1 &

echo "Waiting for common-services (Eureka) to initialize (30s)..."
sleep 30

# ── 5. Start business microservices sequentially ─────────────────────────────
services=(
    "user-service:9902:user_service"
    "product-service:9903:product_service"
    "cart-service:9904:cart_service"
    "order-service:9905:order_service"
    "payment-service:9906:payment_service"
    "wishlist-service:9907:wishlist_service"
    "review-service:9908:review_service"
    "return-service:9909:return_service"
)

for s in "${services[@]}"; do
    IFS=":" read -r name port db_name <<< "$s"
    echo "Starting $name on port $port (database: $db_name)..."
    DB_NAME=$db_name java $JVM_OPTS -jar /app/${name}.jar \
      --server.port=${port} $HIKARI_OPTS > /var/log/${name}.log 2>&1 &
    sleep 20
done

echo ""
echo "✅ EntityKart microservices are running!"
echo "   Eureka Dashboard  : http://localhost:9900"
echo "   Gateway / API     : http://localhost:9900/api/**"
echo "   Notifications API : http://localhost:9900/api/admin/notifications"
echo "   Export API        : http://localhost:9900/api/admin/export"
echo ""
echo "Monitoring logs..."

# Keep container alive
tail -f /var/log/common-services.log \
        /var/log/user-service.log \
        /var/log/product-service.log \
        /var/log/order-service.log
