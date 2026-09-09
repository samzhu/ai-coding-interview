# 設計原則說明

## 雙層 Profile 設計

Spring Boot 的 profile 可以同時啟用多個。將 profile 分成兩個維度，用組合的方式覆蓋不同情境：

| 維度 | 職責 | 範例 |
|------|------|------|
| **基礎設施層** | 連到哪裡（資料庫、訊息佇列、雲端服務） | `local`, `gcp`, `aws` |
| **環境行為層** | 怎麼跑（日誌等級、取樣率、端點開放範圍） | `dev`, `sit`, `uat`, `prod` |

啟動時組合使用：`spring.profiles.active=local,dev` 或 `spring.profiles.active=gcp,sit`。

### 各 Profile 用途

| 類型 | Profile | 用途 |
|------|---------|------|
| 基礎設施 | `local` | 本地開發（Docker Compose、本地資料庫） |
| 基礎設施 | `gcp` | GCP 雲端（Cloud SQL、Secret Manager） |
| 環境行為 | `dev` | 開發環境（DEBUG 日誌、更多 Actuator endpoint） |
| 環境行為 | `sit` | 整合測試（INFO 日誌、50% 取樣） |
| 環境行為 | `uat` | 驗收測試（INFO 日誌、30% 取樣） |

## 檔案分層架構

```
src/main/resources/                    (打包進 Docker Image)
├── application.yaml                   ← 基礎共用配置
├── application-local.yaml             ← 本地基礎設施
└── application-gcp.yaml               ← GCP 基礎設施

config/                                (外部配置，不打包)
├── application-dev.yaml               ← 開發環境行為
├── application-sit.yaml               ← SIT 環境行為
├── application-secrets.properties.example  ← 範例檔（提交 Git）
└── application-secrets.properties     ← 實際機敏值（不提交 Git）
```

**為什麼分兩個目錄？**

- `src/main/resources/` — 打包進 Docker Image，所有環境共用的基礎設定
- `config/` — 外部配置，不進 Image。Spring Boot 會自動讀取工作目錄下的 `config/` 資料夾

## 本地機敏值管理

`config/application-secrets.properties` 是本機開發專用的機敏值檔案，**不提交 Git**。

透過環境行為設定檔（如 `application-dev.yaml`）用 `spring.config.import` 引入：

```yaml
# config/application-dev.yaml
spring:
  config:
    # optional: 前綴讓檔案不存在時不報錯
    import: "optional:file:./config/application-secrets.properties"
```

好處：
- 機敏值（密碼、API Key）不進版本控制
- `.example` 範例檔提供模板，新成員照抄即可上手
- `optional:` 確保沒有這個檔案也能啟動（fallback 到 `application.yaml` 的預設值）
- 雲端環境（GCP）改用 Secret Manager 注入，不需要此檔案

## 統一屬性名稱策略

使用 `{app}-{secret-name}` 格式命名所有外部注入的屬性：

```yaml
spring:
  datasource:
    url: ${myapp-db-url:jdbc:postgresql://localhost:5432/mydb}
    username: ${myapp-db-username:myuser}
    password: ${myapp-db-password:secret}
```

好處：
- 本地用 `application-secrets.properties` 提供值
- GCP 用 Secret Manager 以相同屬性名稱注入
- 有預設值確保本地開發零配置也能跑

## 常見問題對照

| 問題 | 說明 |
|------|------|
| **機敏值硬編碼** | 密碼、API Key 直接寫在設定檔 → 改用統一屬性名稱 + secrets.properties |
| **單一檔案過大** | 所有配置都在 application.yaml → 依職責分拆到對應 profile 檔 |
| **Profile 混亂** | 基礎設施與環境行為混在一起 → 雙層分離 |
| **重複配置** | 同一設定在多處重複 → 提取到 application.yaml 共用 |
| **缺少預設值** | 環境變數沒有預設值 → `${name:default}` 格式 |

## 特殊情況

### 已有多環境配置

1. 識別哪些是基礎設施、哪些是環境行為
2. 重新分類到對應檔案
3. 提取共用配置到 application.yaml

### 使用 Spring Cloud Config

1. 保留 bootstrap.yaml
2. application.yaml 仍遵循分層原則
3. 遠端配置覆蓋本地配置

### 多模組專案

1. 根模組放共用配置
2. 子模組放模組特定配置
3. 使用 `spring.config.import` 引入

## 參考資源

- [Spring Boot Externalized Configuration](https://docs.spring.io/spring-boot/reference/features/external-config.html)
- [Spring Boot Profiles](https://docs.spring.io/spring-boot/reference/features/profiles.html)
- [Spring Boot Docker Compose Support](https://docs.spring.io/spring-boot/reference/features/dev-services.html)
