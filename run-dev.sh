#!/bin/sh
# Invoicely - Local Development Setup Script
# Starts PostgreSQL in Docker and runs the application

set -e

APP_DIR="$(cd "$(dirname "$0")" && pwd)"
DB_CONTAINER="invoicely-db"
DB_NAME="invoicely"
DB_USER="postgres"
DB_PASSWORD="postgres"
DB_PORT="5432"

echo "=== Invoicely Local Setup ==="
echo ""

# --- Step 1: Start PostgreSQL ---
echo "[1/3] Setting up PostgreSQL..."

if docker ps --format '{{.Names}}' | grep -q "^${DB_CONTAINER}$"; then
    echo "  PostgreSQL container already running."
elif docker ps -a --format '{{.Names}}' | grep -q "^${DB_CONTAINER}$"; then
    echo "  Starting existing PostgreSQL container..."
    docker start "$DB_CONTAINER"
else
    echo "  Creating new PostgreSQL container..."
    docker run -d \
        --name "$DB_CONTAINER" \
        -e POSTGRES_DB="$DB_NAME" \
        -e POSTGRES_USER="$DB_USER" \
        -e POSTGRES_PASSWORD="$DB_PASSWORD" \
        -p "$DB_PORT":5432 \
        postgres:16-alpine
    echo "  Waiting for PostgreSQL to be ready..."
    sleep 3
fi

# Verify PostgreSQL is accepting connections
for i in 1 2 3 4 5; do
    if docker exec "$DB_CONTAINER" pg_isready -U "$DB_USER" > /dev/null 2>&1; then
        echo "  PostgreSQL is ready."
        break
    fi
    if [ "$i" -eq 5 ]; then
        echo "  ERROR: PostgreSQL failed to start. Check: docker logs $DB_CONTAINER"
        exit 1
    fi
    sleep 2
done

# --- Step 2: Ready ---
echo ""
echo "[2/2] Setup complete!"
echo ""
echo "  DB:  postgresql://localhost:${DB_PORT}/${DB_NAME}"
echo ""
echo "  To run the app:"
echo "    cd $APP_DIR && mvn spring-boot:run -DskipTests -s .mvn/local-settings.xml"
echo ""
