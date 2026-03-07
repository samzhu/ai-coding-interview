# Backend — AI Coding Interview

## 前置需求

| 工具 | 版本 |
|------|------|
| Java | 21（LTS）以上 |
| Docker Desktop | 任意最新版（需在背景執行） |

> `./gradlew bootRun` 會透過 **spring-boot-docker-compose** 自動啟動 `compose.yaml` 中定義的容器（PostgreSQL），不需要手動執行 `docker compose up`。

---

## 快速啟動

```bash
cd backend
./gradlew bootRun
```

Spring Boot 啟動時會自動執行：
1. 啟動 `compose.yaml` 中的 **PostgreSQL**（port 5432）
2. 執行 Liquibase 資料庫遷移
3. 掃描並載入 `resources/questions/` 下的題目定義

---

## Docker 執行模式

程式碼執行（`execution` 模組）使用 Docker 容器隔離執行環境。`application.yml` 預設透過 **Unix socket** 連線本機 Docker（OrbStack / Docker Desktop）。

### A. Local Docker Socket（預設）

`application.yml` 預設值：

```yaml
execution:
  docker:
    host: ${DOCKER_HOST:unix:///var/run/docker.sock}
```

本地開發直接使用 OrbStack 或 Docker Desktop 提供的 socket，不需要額外的 DinD 容器。

**首次使用需確認執行 image 已在本機：**

```bash
docker pull eclipse-temurin:25-jdk
```

### B. Docker Remote TCP（生產環境）

生產環境或 CI 需要連線遠端 Docker daemon 時，透過環境變數覆蓋：

```bash
DOCKER_HOST=tcp://<host>:2375 ./gradlew bootRun
```

或在部署環境設定 `DOCKER_HOST` 環境變數。

---

## 前端 Dev Server

```bash
# 受測者 App（port 3001）
cd frontend && npm run dev:candidate

# 面試官 App（port 3000）
cd frontend && npm run dev:admin
```

---

## 環境變數速查表

| 變數名稱 | 預設值 | 說明 |
|----------|--------|------|
| `DOCKER_HOST` | `unix:///var/run/docker.sock` | Docker 連線位址；生產環境改為 TCP 位址（如 `tcp://<host>:2375`） |
| `GOOGLE_GENAI_API_KEY` | （空）| Google Gemini API 金鑰 |
| `ADMIN_TOKEN` | （空）| Admin API 存取 token |

---

## 執行測試

```bash
cd backend
./gradlew test
```

測試使用 `application-test.yml`（`unix:///var/run/docker.sock`），直接連線本機 Docker，**不經過 DinD**。

```bash
# 只跑 BDD 場景
./gradlew test --tests "com.interview.*.bdd.*"

# 只跑特定模組
./gradlew test --tests "com.interview.interview.domain.*"
```
