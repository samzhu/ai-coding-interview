# Spring Boot 設定檔模板

## application.yaml（基礎共用配置）

```yaml
# =============================================================================
# {App Name} - 基礎共用配置
# =============================================================================
# 此檔案包進 Docker Image，包含所有環境共用的配置
# 環境特定配置透過 profile 檔案覆蓋
# =============================================================================

spring:
  application:
    name: {App Name}

  profiles:
    default: local,dev

  threads:
    virtual:
      enabled: true

  # ----- 資料庫配置 -----
  datasource:
    url: ${myapp-db-url:jdbc:postgresql://localhost:5432/mydb}
    username: ${myapp-db-username:myuser}
    password: ${myapp-db-password:secret}

# ----- Actuator 配置 -----
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: always
      probes:
        enabled: true

# ----- 日誌配置 -----
logging:
  level:
    root: INFO
```

## application-local.yaml（本地基礎設施）

```yaml
# =============================================================================
# 本地開發基礎設施
# =============================================================================
# Profile: local
# 用途: 本地開發環境，使用 Docker Compose
# =============================================================================

spring:
  docker:
    compose:
      enabled: true
      lifecycle-management: start-and-stop
```

## config/application-dev.yaml（開發環境行為）

```yaml
# =============================================================================
# 開發環境行為配置
# =============================================================================
# Profile: dev
# 用途: 本地開發環境，DEBUG 日誌
# =============================================================================

spring:
  config:
    import: "optional:file:./config/application-secrets.properties"

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,env,configprops

logging:
  level:
    com.example.myapp: DEBUG
```

## config/application-secrets.properties.example

```properties
# =============================================================================
# 本地開發機敏設定 (此檔案不提交到 Git)
# =============================================================================
# 使用方式:
#   1. 複製此檔案: cp application-secrets.properties.example application-secrets.properties
#   2. 編輯填入實際值
#   3. 啟動應用程式
# =============================================================================

myapp-db-url=jdbc:postgresql://localhost:5432/mydb
myapp-db-username=myuser
myapp-db-password=your-password-here
myapp-google-api-key=your-api-key-here
```

## .gitignore 追加

```gitignore
### Secrets ###
config/application-secrets.properties
!config/application-secrets.properties.example
```

## 完成報告模板

```markdown
## 整理完成

### 檔案結構

src/main/resources/
├── application.yaml          ← 基礎共用配置
└── application-local.yaml    ← 本地基礎設施

config/
├── application-dev.yaml      ← 開發環境行為
└── application-secrets.properties.example  ← 範例檔

### 統一屬性名稱

| 屬性名稱 | 用途 | 預設值 |
|----------|------|--------|
| myapp-db-url | 資料庫 URL | ... |
| myapp-db-username | 資料庫使用者 | ... |
| myapp-db-password | 資料庫密碼 | (無) |

### 本地開發啟動

cp config/application-secrets.properties.example config/application-secrets.properties
# 編輯填入實際值
./gradlew bootRun

### 後續可擴展

- 新增 application-gcp.yaml 支援 GCP
- 新增 config/application-sit.yaml 支援 SIT 環境
```
