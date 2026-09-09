# Frontend

npm Workspaces monorepo，使用 Next.js 16.1.6、React 19 與共用套件 @interview/shared。完整設定先讀 [本機建置與操作手冊](../docs/getting-started-local.md)。

## 開發模式

準備 Node 24/npm，先啟動 Backend 8080。以下在 frontend/ 執行：

```bash
npm ci
NEXT_PUBLIC_CANDIDATE_URL=http://localhost:3001 npm run dev:admin
```

另一個 terminal，在 frontend/ 執行：

```bash
npm run dev:candidate
```

Admin 使用 3000、Candidate 使用 3001。預設 HTTP API 經 Next.js rewrite 到 Backend，WebSocket 直接連 Backend。

## 建置

在 frontend/ 執行：

```bash
npm run build:admin
npm run build:candidate
npm run build:export:admin
npm run build:export:candidate
```

Docker 使用 static export + Nginx。NEXT_PUBLIC_API_BASE 與 Admin 的 NEXT_PUBLIC_CANDIDATE_URL 在 build 時寫入靜態檔；修改後需要重建前端 image。

## 目錄

- apps/admin：建立面試、題目清單與監看。
- apps/candidate：候選人 editor、terminal、測試與 AI。
- packages/shared：共用 API client、型別與 UI。
