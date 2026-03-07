# Backend — AI Coding Interview

## 前置需求

| 工具 | 版本 |
|------|------|
| Java | 21（LTS）以上 |
| Docker Desktop | 任意最新版（需在背景執行） |

> `./gradlew bootRun` 會透過 **spring-boot-docker-compose** 自動啟動 `compose.yaml` 中定義的容器（PostgreSQL + DinD），不需要手動執行 `docker compose up`。

---

## 快速啟動

```bash
cd backend
./gradlew bootRun
```

Spring Boot 啟動時會自動執行：
1. 啟動 `compose.yaml` 中的 **PostgreSQL**（port 5432）和 **DinD**（Docker-in-Docker，port 2375）
2. 執行 Liquibase 資料庫遷移
3. 掃描並載入 `resources/questions/` 下的題目定義

---

## Docker 執行模式

程式碼執行（`execution` 模組）使用 Docker 容器隔離執行環境。`application.yml` 預設連線至 **DinD（Docker Remote）**，模擬正式部署環境。

### A. Docker Remote（預設）

`application.yml` 預設值：

```yaml
execution:
  docker:
    host: ${DOCKER_HOST:tcp://localhost:2375}
```

DinD 容器在 `compose.yaml` 中已定義，`bootRun` 時自動啟動。

**首次使用需將執行 image pull 進 DinD：**

```bash
# 確認 DinD 可連線
docker -H tcp://localhost:2375 info

# Pull 題目執行用的 image
docker -H tcp://localhost:2375 pull eclipse-temurin:25-jdk
```

> 每次 DinD 容器重建（`docker compose down`）後需重新 pull image，因為 DinD 的 image 儲存在容器內部。

### B. Local Docker Socket（覆蓋方式）

如果想直接使用本機 Docker（跳過 DinD），可在啟動時覆蓋環境變數：

```bash
DOCKER_HOST=unix:///var/run/docker.sock ./gradlew bootRun
```

或建立 `.env` / IDE run configuration 設定 `DOCKER_HOST`。

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
| `DOCKER_HOST` | `tcp://localhost:2375` | Docker 連線位址 |
| `DOCKER_TLS_VERIFY` | `false` | TLS 驗證（Remote TLS 用） |
| `DOCKER_CERT_PATH` | （空）| TLS 憑證路徑 |
| `EXECUTION_DOCKER_IMAGE` | `eclipse-temurin:25-jdk` | 程式碼執行容器 image |
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
