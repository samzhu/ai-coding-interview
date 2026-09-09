# AI 程式面試平台

提供面試官管理後台與候選人工作區，支援 Docker 考題環境、分階段測試、程式編輯、終端機及 AI 對話。

第一次使用請先閱讀 **[開發者本機建置與操作手冊](docs/getting-started-local.md)**，依序完成環境設定、考題建置、本機部署與驗收。

## 本機啟動

建置需要 JDK 25 與正在執行的 Docker；前端開發模式另外需要 Node 24/npm。

本機測試 repo 的考題時，先從根目錄編譯：

```bash
docker build -t spike19820318/ai-coding-interview-question01:latest exams/100
```

接著依手冊第 3 章建置平台、將考題 image 載入 DinD，最後啟動服務。Host Docker 與 DinD 的 image cache 不共用。

`./build-and-run.sh` 可建置平台並下載遠端現成考題，但不會編譯 `exams/100`；要驗證本機考題修改，請走手冊主線。

| 入口 | 網址 |
|---|---|
| 面試官 | http://localhost:3000 |
| 候選人 | http://localhost:3001（使用面試邀請連結進入） |
| Backend | http://localhost:8080 |

## 專案結構

- `backend/`：Spring Boot 4、Java 25、Spring Data JDBC、Liquibase。
- `frontend/apps/admin/`：面試官 Next.js app。
- `frontend/apps/candidate/`：候選人 Next.js app。
- `frontend/packages/shared/`：共用型別、API client 與元件。
- `exams/100/`：Hangman 考題、Dockerfile 與 exam.yml。
- `docker-compose.yml`：本機完整環境（PostgreSQL、DinD、Backend、兩個前端）。
- `docs/`：開發與部署文件。

## 設定與開發

API key、模型清單、Docker 與 bootRun 的設定位置不同，詳見手冊第 2、5 章。真實 key 放未提交的 .env 或 secrets 檔案。

- [本機建置與操作手冊](docs/getting-started-local.md)
- [Backend 開發指令](backend/README.md)
- [Frontend 開發指令](frontend/README.md)
- [本機部署查核依據](docs/infrastructure/local-setup-verification.md)

本機 Compose 關閉 OAuth2，供本機驗收使用；正式部署需另外配置認證與網路。
