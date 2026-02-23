#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
FRONTEND_DIR="$SCRIPT_DIR/frontend"
BACKEND_DIR="$SCRIPT_DIR/backend"

# 載入環境變數（docker compose 會用到 POSTGRES_PASSWORD 等）
if [ -f "$SCRIPT_DIR/.env" ]; then
  set -a; source "$SCRIPT_DIR/.env"; set +a
fi

echo "=== Step 1: Build backend Docker image (Buildpacks) ==="
cd "$BACKEND_DIR"
./gradlew bootBuildImage -x test

echo "=== Step 2: Build frontend Docker image (Nginx) ==="
cd "$FRONTEND_DIR"
docker build -t ai-coding-interview-frontend:latest .

echo "=== Step 3: Start docker compose ==="
cd "$SCRIPT_DIR"
docker compose -f docker-compose.yml up -d

echo "=== Step 4: Waiting for backend to be ready ==="
for i in $(seq 1 30); do
  if curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo "Backend is ready!"
    break
  fi
  if [ "$i" -eq 30 ]; then
    echo "WARNING: Backend did not become ready within 60s. Check logs: docker compose logs backend"
  fi
  sleep 2
done

echo ""
echo "=== Done! ==="
echo "App:  http://localhost:3000"
echo "API:  http://localhost:8080"
echo "Logs: docker compose logs -f"
