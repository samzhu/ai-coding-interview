# 本機建置手冊查核備忘錄

查核日期：2026-09-09。依目前工作目錄原始碼與 Docker 官方文件核對；未重新執行完整 image build 或面試端到端測試。

## Docker 官方語意

- `docker image save` 將 image 輸出為 archive（預設 stdout）；`docker image load` 可讀 stdin，還原 image 與 tag。因此 `docker save IMAGE | docker compose exec -T dind docker load` 可把宿主機 image 匯入 DinD 的 Docker daemon。來源：[save](https://docs.docker.com/reference/cli/docker/image/save/)、[load](https://docs.docker.com/reference/cli/docker/image/load/)。
- `docker compose up --wait --wait-timeout 120` 等待服務達到 running／healthy，並隱含 detached 模式。沒有 healthcheck 的服務只保證 running；本 repo backend 沒有 healthcheck，仍須另外檢查 `/actuator/health`。來源：[Compose up](https://docs.docker.com/reference/cli/docker/compose/up/)、[本機 Compose](../../docker-compose.yml)。
- `docker compose restart` 不套用 Compose 設定或環境變數變更；更新 API key 後用 `docker compose up -d --force-recreate backend`。來源：[Compose restart](https://docs.docker.com/reference/cli/docker/compose/restart/)、[Compose up](https://docs.docker.com/reference/cli/docker/compose/up/)。

## 本 repo 考題 image

- [exams/100/Dockerfile](../../exams/100/Dockerfile) 使用 JDK 25、`WORKDIR /workspace`、`COPY . .`，執行 `./gradlew testClasses --no-daemon`；這是編譯測試與預熱依賴，並未執行關卡測試。
- [question01.yaml](../../backend/src/main/resources/questions/question01.yaml) 指定 `spike19820318/ai-coding-interview-question01:latest`；本機 build 的 tag 必須與此一致，或同步調整 metadata 並重建 backend。
- [exams/100/exam.yml](../../exams/100/exam.yml) 有 3 個 checkpoint：第 1 個跑 CP1Test、第 2 個跑 CP2Test、第 3 個同時跑 CP3Test 與 CP4Test。不可把 4 個 Java test class 誤寫成 4 個關卡。
- [ExamConfigService](../../backend/src/main/java/com/interview/execution/ExamConfigService.java) 固定從 `/workspace/exam.yml` 讀取設定；即使 YAML 有 `workspace`，也不改變設定檔本身的讀取位置。設定按 container ID 快取，且自動排除檔案瀏覽器中的 `exam.yml`。
- [DockerContainerManager](../../backend/src/main/java/com/interview/execution/internal/DockerContainerManager.java) 在指定 Docker daemon 上先 inspect image，只有不存在才 pull；已有同 tag 的舊 image 不會自動更新。建立容器時把 command 設為 `tail -f /dev/null`。
- [root Compose](../../docker-compose.yml) 令 backend 使用 `tcp://dind:2375`。宿主機 build 成功後仍須 load 到 DinD。image 更新後既有容器不會換底層 image，請建立新面試驗證。
- [build-and-run.sh](../../build-and-run.sh) 不 build `exams/100`；它在服務啟動後 pull registry 題目到 DinD。因此使用本機考題時應在腳本結束後重新 load，或按手冊手動啟動流程，避免後續 pull 覆蓋本機 tag。

## 建議手冊主線

先介紹 metadata／image／exam.yml 的責任，再準備環境與 key，build `exams/100`，build 平台，啟動並等待 DinD，load 題目，確認 backend health，最後建立新面試及執行 checkpoint。將 bootRun 留在完成首次完整 Docker 操作後的開發迭代章節。
