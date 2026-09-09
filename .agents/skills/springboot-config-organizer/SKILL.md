---
name: springboot-config-organizer
description: >-
  Organize and restructure Spring Boot configuration files using dual-layer
  profile design (infrastructure + environment behavior). Use when the user
  wants to organize, restructure, or review Spring Boot config files
  (application.yaml, application.properties, profile configs). Trigger on:
  "config", "profile", "yaml", "properties", "environment config",
  "externalized configuration", "secrets management", "整理設定檔",
  "設定檔太亂", "config 分層", "機敏值", "環境變數", "配置".
  Also use when discussing Spring Boot secret handling or multi-environment
  setup.
metadata:
  author: samzhu
  version: 1.2.0
---

# Spring Boot 設定檔整理

## Instructions

### Step 1: 分析現有設定檔

1. 找出所有設定檔：
   ```
   Glob: **/application*.yaml
   Glob: **/application*.yml
   Glob: **/application*.properties
   ```
2. 逐一讀取，記錄：
   - 檔案結構與 Profile 使用方式
   - 機敏值位置（密碼、API Key）
   - 資料庫、第三方服務、日誌、Actuator 配置
   - 重複配置與環境變數使用方式
3. 識別問題（機敏值硬編碼、單檔過大、Profile 混亂、重複配置、缺少預設值）

### Step 2: 規劃新架構

1. 將現有配置分類到對應檔案：
   - `application.yaml` — 共用配置（app name、datasource、actuator、logging root）
   - `application-local.yaml` — 本地基礎設施（Docker Compose）
   - `application-gcp.yaml` — GCP 基礎設施（Secret Manager、Cloud SQL）
   - `config/application-dev.yaml` — 開發環境行為（DEBUG 日誌、引入 secrets）
   - `config/application-secrets.properties` — 本機機敏值（不提交 Git）
2. 建立統一屬性名稱對照表，使用 `{app}-{secret-name}` 格式

設計原則見 `references/design-principles.md`。

### Step 3: 與用戶確認

向用戶報告分析結果和建議的新結構，確認：
1. 應用程式名稱前綴（統一屬性名稱用）
2. 需要哪些基礎設施 profile（local、gcp、aws...）
3. 需要哪些環境 profile（dev、sit、uat、prod...）

**等待用戶確認後再執行。**

### Step 4: 執行整理

依序建立以下檔案，模板見 `references/config-templates.md`：

1. `mkdir -p config`
2. 建立 `application.yaml` — 機敏值用 `${app-xxx:預設值}` 格式
3. 建立 `application-local.yaml`
4. 建立 `config/application-dev.yaml` — 用 `spring.config.import` 引入 secrets：
   ```yaml
   spring:
     config:
       import: "optional:file:./config/application-secrets.properties"
   ```
5. 建立 `config/application-secrets.properties.example`
6. 更新 `.gitignore` — 排除 `config/application-secrets.properties`

### Step 5: 驗證

1. 檢查檔案結構是否符合規劃
2. 執行 `./gradlew bootRun` 或 `./mvnw spring-boot:run`
3. 確認啟動日誌顯示正確的 active profiles
4. 產出完成報告（模板見 `references/config-templates.md`）

## Checklist

```
□ application.yaml 只包含共用配置
□ 機敏值使用統一屬性名稱 ${app-xxx}
□ config/application-dev.yaml 用 spring.config.import 引入 secrets
□ config/application-secrets.properties.example 有完整範例
□ .gitignore 排除 secrets.properties
□ 啟動測試通過
□ Profile 正確載入
```

## Deep Dives

- `references/design-principles.md` — 雙層 Profile 設計原則、機敏值管理策略、特殊情況處理
- `references/config-templates.md` — 所有設定檔的完整模板和完成報告模板
