#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
BACKEND_DIR="$SCRIPT_DIR/backend"

# 載入環境變數（docker compose 會用到 POSTGRES_PASSWORD 等）
if [ -f "$SCRIPT_DIR/.env" ]; then
  set -a; source "$SCRIPT_DIR/.env"; set +a
fi

echo "=== Step 1: Build backend Docker image (Buildpacks) ==="
cd "$BACKEND_DIR"
./gradlew bootBuildImage -x test

echo "=== Step 2: Build frontend Docker images ==="
cd "$SCRIPT_DIR"
docker compose build admin candidate

echo "=== Step 3: Start services ==="
docker compose up -d

echo "=== Step 4: Pre-pull execution images into DinD ==="
echo "Waiting for DinD to be ready..."
for i in $(seq 1 30); do
  if docker compose exec dind docker info > /dev/null 2>&1; then
    break
  fi
  sleep 2
done
docker compose exec dind docker pull spike19820318/ai-coding-interview-question01:latest
echo "Execution images ready."

echo "=== Step 5: Waiting for backend to be ready ==="
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
echo "Admin:     http://localhost:3000"
echo "Candidate: http://localhost:3001"
echo "API:       http://localhost:8080"
echo "Logs:      docker compose logs -f"
