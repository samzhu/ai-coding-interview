# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

你上網查證一下比在那邊試要快
要習慣紀錄問題跟解法, 或是為什麼考慮這樣處理

## Project Structure

```
ai-coding-interview/
├── .claude/              # Claude Code 設定 + Skills
├── .gitignore
├── CLAUDE.md
├── LICENSE
├── docs/                 # 技術文件
├── backend/              # Spring Boot 後端
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   ├── gradlew, gradlew.bat
│   ├── gradle/
│   ├── compose.yaml      # PostgreSQL Docker Compose
│   └── src/
└── frontend/             # npm Workspaces Monorepo
    ├── package.json          # workspace root
    ├── packages/
    │   └── shared/           # @interview/shared（型別、API client、UI 元件）
    └── apps/
        ├── admin/            # 面試官 App（port 3000）
        └── candidate/        # 受測者 App（port 3001）
```

## Backend Commands

```bash
# Run all tests (requires Docker for Testcontainers)
cd backend && ./gradlew test

# Run a single test class
cd backend && ./gradlew test --tests "com.interview.interview.domain.InterviewStatusTest"

# Run tests by category (fast, no Docker)
cd backend && ./gradlew test --tests "com.interview.interview.domain.*" --tests "com.interview.interview.application.*"

# Run only Cucumber BDD scenarios
cd backend && ./gradlew test --tests "com.interview.interview.bdd.CucumberTestSuite"

# Run only Modulith verification
cd backend && ./gradlew test --tests "com.interview.ModularityTests"

# Build without tests
cd backend && ./gradlew build -x test

# Start local dev environment (PostgreSQL via Docker Compose)
cd backend && ./gradlew bootRun
# Spring Boot auto-starts compose.yaml (spring-boot-docker-compose)
```

## Frontend Commands

Frontend is now an npm Workspaces monorepo with two apps:
- `apps/admin/` — 面試官 App (port 3000)
- `apps/candidate/` — 受測者 App (port 3001)
- `packages/shared/` — 共用型別、API client、UI 元件

```bash
# Install dependencies (workspace root)
cd frontend && npm install

# Start dev servers
cd frontend && npm run dev:admin        # 面試官 App (port 3000)
cd frontend && npm run dev:candidate    # 受測者 App (port 3001)

# Build for production
cd frontend && npm run build:admin
cd frontend && npm run build:candidate

# Build static export (for Docker / nginx)
cd frontend && npm run build:export:admin
cd frontend && npm run build:export:candidate
```

## E2E Validation

```bash
# Terminal 1: 後端
cd backend && ./gradlew bootRun

# Terminal 2: 面試官 App (port 3000)
cd frontend && npm run dev:admin

# Terminal 3: 受測者 App (port 3001)
cd frontend && npm run dev:candidate

# 1. Open http://localhost:3000/interviews/new
# 2. Create interview → copy invitation link (points to http://localhost:3001)
# 3. Open invitation link in new tab → join interview
# 4. Write code → Submit → view test results
# 5. Chat with AI
# 6. All checkpoints passed → completion page
# 7. Admin monitor: http://localhost:3000/interviews/{id}/monitor
```

## Backend Architecture

**Spring Modulith modular monolith** rooted at `com.interview`. Each sub-package under the root is a Spring Modulith module.

Module layout (`backend/src/main/java/com/interview/<module>/`):

```
<module>/
├── domain/          # Aggregate roots, value objects, domain events, enums
├── application/     # Use-case services (@Service), command records
├── infrastructure/  # Spring Data JDBC repositories
└── interfaces/rest/ # @RestController, request/response records, exception handler
```

**`ModularityTests`** at `backend/src/test/java/com/interview/` enforces module boundaries via `ApplicationModules.of(InterviewPlatformApplication.class).verify()`.

## Key Technology Decisions

| Concern | Choice | Notes |
|---------|--------|-------|
| Data layer | **Spring Data JDBC** (not JPA) | `@Table`, `@Id`, `@Version` — no lazy loading, no `@Entity` |
| Schema migration | **Liquibase** | Master: `db/changelog/db.changelog-master.yaml`, SQL files in same dir |
| Event publication store | **Spring Modulith JDBC** | Requires `event_publication` table — created by `002-create-event-publication-table.sql` |
| Test DB | **Testcontainers 2.0** | `@ServiceConnection` auto-wires; no JDBC URL needed in test config |
| BDD | **Cucumber 7** + JUnit Platform Suite | Feature files in `backend/src/test/resources/features/` |
| Frontend framework | **Next.js 16.1** | App Router, TypeScript, Tailwind CSS 4 |
| Code editor | **CodeMirror 6** | `@uiw/react-codemirror`, dynamic import + ssr: false |
| UI components | **shadcn/ui** | Base for all UI components |

## Domain Model Patterns

**`Interview` aggregate** (`backend/src/main/java/com/interview/interview/domain/Interview.java`):
- Created only via `Interview.schedule(...)` factory method — constructor is `protected`
- State stored as `String status` internally; exposed as `InterviewStatus getInterviewStatus()`
- `@Version Long version` — required for Spring Data JDBC to distinguish new entities (version=null → INSERT) from existing ones (version≥0 → UPDATE)
- State transitions enforced by `InterviewStatus.canTransitionTo()` — throws `IllegalStateException` on invalid transitions

**Events**: `InterviewCompletedEvent` published via `ApplicationEventPublisher` in `InterviewService.completeInterview()`.

## Testing Strategy

| Layer | Annotation | Notes |
|-------|-----------|-------|
| Domain unit | JUnit 5 + AssertJ | No Spring context |
| Service unit | `@ExtendWith(MockitoExtension.class)` | Mocked repo + `ApplicationEventPublisher` |
| Repository integration | `@DataJdbcTest` | Spring Boot 4 path: `org.springframework.boot.data.jdbc.test.autoconfigure` |
| Controller slice | `@WebMvcTest` | Spring Boot 4 path: `org.springframework.boot.webmvc.test.autoconfigure` |
| BDD scenarios | `CucumberTestSuite` (@Suite) | Full `@SpringBootTest` + Testcontainers |
| Modulith | `ModularityTests` | Verifies + generates PlantUML docs in `build/` |

**Shared Testcontainers config**: `TestcontainersConfiguration` (`backend/src/test/java/com/interview/`) — import it via `@Import(TestcontainersConfiguration.class)`. Uses Testcontainers 2.x API (`org.testcontainers.postgresql.PostgreSQLContainer`, no generic type parameter).

**BDD event capture**: `CapturedEvents` (`backend/src/test/java/com/interview/interview/bdd/`) — `@Component` that listens for `InterviewCompletedEvent`; imported in `CucumberSpringContextConfiguration`. Step definitions use `UUID.nameUUIDFromBytes(str.getBytes())` to convert human-readable IDs like `"candidate-001"` to deterministic UUIDs.

## Spring Boot 4 / Jackson 3 Gotchas

- `ObjectMapper` import: `tools.jackson.databind.ObjectMapper` (not `com.fasterxml.jackson`)
- `@WebMvcTest` import: `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`
- `@DataJdbcTest` import: `org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest`
- Testcontainers 2.0: artifacts renamed to `testcontainers-junit-jupiter` / `testcontainers-postgresql`
- `@MockBean` → use `@MockitoBean` (from `org.springframework.test.context.bean.override.mockito`)
- `MockMvc.perform(...).content(bytes)` — prefer `objectMapper.writeValueAsBytes()` over `writeValueAsString()`

## Adding a New Backend Module

1. Create package `com.interview.<module-name>` with `domain/`, `application/`, `infrastructure/`, `interfaces/rest/` sub-packages
2. Add Liquibase SQL in `backend/src/main/resources/db/changelog/` and include it in `db.changelog-master.yaml`
3. Add Gherkin scenarios in `backend/src/test/resources/features/`
4. `ModularityTests` will automatically pick up the new module and verify its boundaries

## Frontend Architecture

```
frontend/src/
├── app/
│   ├── api/                    # Route Handlers (proxy to backend)
│   ├── admin/                  # HR admin pages
│   ├── invite/[token]/         # Candidate invitation flow
│   ├── interview/[id]/         # Interview workspace
│   └── page.tsx                # Home page
├── components/
│   ├── admin/                  # Admin-specific components
│   ├── code-editor/            # CodeMirror 6 wrapper
│   └── interview/              # Interview workspace components
├── contexts/                   # React Context providers
├── hooks/                      # Custom React hooks
├── lib/                        # Utilities (api-proxy, api-client)
└── types/                      # TypeScript type definitions
```

## AI 協作開發流程

> 參考文件：`docs/testing/bdd-tdd-ai-workflow.md`

BDD 與 TDD 是兩種**獨立**的實踐，目的不同，不要混用：

| | BDD（`/bdd`）| TDD（`/tdd`）|
|---|---|---|
| 目的 | 定義行為規格、確認驗收條件 | 保護既有邏輯、防止改壞 |
| 時機 | 新功能開發（主要流程）| 修改程式碼前（保護機制）|
| 協作 | 業務 + 開發 + 測試三方 | 開發者個人 |

### /bdd — 新功能主要流程

**觸發條件（任一即呼叫）：**
```
✅ 開發新的業務功能 / API 端點
✅ 修改既有業務行為
✅ 需要確認驗收條件或邊界情況
```

**三階段：**
```
Phase 1 Discovery — Example Mapping 四色卡片法
  🟡 Story → 🔵 Rules → 🟢 Examples → 🔴 Questions
  目標：🔴 Questions 歸零後才進 Phase 2

Phase 2 Formulation — 撰寫 Gherkin 場景
  將 🟢 Examples → Given-When-Then
  ⚠️ 用戶確認後才進 Phase 3

Phase 3 Automation — 實作讓場景通過
  實作 Step Definitions → Cucumber PASSED
```

**本專案檔案位置：**

| 產出物 | 路徑 |
|--------|------|
| Feature 檔 | `backend/src/test/resources/features/<module>.feature` |
| Step 定義 | `backend/src/test/java/com/interview/<module>/bdd/` |
| BDD Runner | `backend/src/test/java/com/interview/<module>/bdd/CucumberTestSuite.java` |

```bash
cd backend && ./gradlew test --tests "com.interview.*.bdd.*"
```

### /tdd — 修改前的保護機制

**觸發條件：**
```
✅ 修復 Bug（先寫重現測試，再修）
✅ 重構既有程式碼（確保行為不變）
✅ 前端 Hook / Component 需要邏輯保護時
```

```bash
cd backend && ./gradlew test
```

---

## 可用 Skills

| Skill | 用途 | 觸發詞 |
|-------|------|--------|
| `/research` | 研究技術主題並整理教學文件 | 研究、調查、比較、分析、了解 |
| `/bdd` | 行為驅動開發 — 從需求探索到 Gherkin 場景 | BDD、行為驅動、Gherkin、使用者故事、驗收條件 |
| `/tdd` | 測試驅動開發 — 紅綠重構循環 | TDD、測試驅動、先寫測試、紅綠重構 |
| `/ui-craft` | 有意圖的 UI 設計與建構 | UI、介面設計、dashboard、元件、頁面設計 |

## 專案慣例

### 文件管理

- 教學文件放在 `docs/` 目錄
- 新增文件後必須更新 `docs/INDEX.md`
- 檔案命名使用小寫英文加連字號（如 `event-sourcing.md`）
- 分類結構以 `docs/INDEX.md` 的「分類說明」為準
