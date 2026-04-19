#!/usr/bin/env bash
# Create all required Kafka topics.
set -euo pipefail

KAFKA_CONTAINER="${KAFKA_CONTAINER:-cricklive-kafka}"

topics=(
  "ball-events:3:1"
  "match-state-changes:3:1"
  "commentary-events:3:1"
  "notification-events:3:1"
  "stats-update-events:3:1"
)

for entry in "${topics[@]}"; do
  IFS=: read -r topic partitions replication <<< "$entry"
  echo "Creating topic: $topic (partitions=$partitions, replication=$replication)"
  docker exec "$KAFKA_CONTAINER" kafka-topics \
    --bootstrap-server localhost:9092 \
    --create \
    --if-not-exists \
    --topic "$topic" \
    --partitions "$partitions" \
    --replication-factor "$replication"
done

echo "Done. Existing topics:"
docker exec "$KAFKA_CONTAINER" kafka-topics --bootstrap-server localhost:9092 --list
