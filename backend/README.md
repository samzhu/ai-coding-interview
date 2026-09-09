# Backend

Spring Boot 4.0.5、Java 25、Gradle wrapper 9.3.0。第一次使用先讀 [本機建置與操作手冊](../docs/getting-started-local.md)，包含 Docker 與 bootRun 兩種方式。

## 本機開發

先停止 root Compose，避免 PostgreSQL 5432 與 Backend 8080 衝突。以下從 repo 根目錄執行：

```bash
docker compose stop
docker build -t spike19820318/ai-coding-interview-question01:latest exams/100
cd backend
./gradlew bootRun
```

bootRun 會使用 backend/compose.yaml 啟動 PostgreSQL，執行 Liquibase 並載入 classpath questions/*.yaml。考題執行使用 host Docker，預設 unix:///var/run/docker.sock。

在 backend/config/application-secrets.properties 填寫設定；外部 dev profile 會載入它：

```properties
aci-security-enabled=false
aci-google-genai-api-key=
aci-anthropic-api-key=
aci-openai-api-key=
```

這份檔案不提交。Docker 模式不會自動讀取它，請參考手冊的 Compose env 設定。

## 建置與測試

以下在 backend/ 執行：

```bash
./gradlew test
./gradlew test --tests "com.interview.interview.domain.*"
./gradlew bootBuildImage -x test
```

整合測試需要 Docker/Testcontainers。bootBuildImage 輸出 ai-coding-interview-backend:latest；-x test 表示略過測試。

模型清單在 src/main/resources/application.yaml 的 aci.models。改 Java 或 resources 後，Docker 模式需要重建 Backend image 並 recreate；詳見手冊第 6 章。
