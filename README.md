# AI Coding Interview Platform

> An open-source, enterprise-grade coding interview platform inspired by **Meta's 2025 AI-Enabled Coding Interview** format — multi-file project workspace, governed AI assistant, and Checkpoint-driven scoring in a single seamless experience.

[English](./README.md) · [繁體中文](./README.zh-TW.md)

---

## Why this project?

In late 2025, Meta became the first FAANG company to officially adopt **AI-assisted coding interviews**, reflecting the reality that 82% of developers already use AI tools at work. The interview is no longer "how fast can you code alone" but **"how well do you think, prompt, and collaborate with AI while keeping engineering judgment intact"**.

The market gap is clear: no off-the-shelf product offers a unified experience covering a real multi-file IDE, multiple AI models, real-time interviewer monitoring, AI-interaction auditing, and structured Checkpoint grading. Meta had to heavily customize CoderPad — most companies don't have those engineering resources.

**This project closes that gap.** It's a self-hostable platform that mirrors Meta's interview format end-to-end, so any team can evaluate candidates the way modern engineers actually work.

---

## Screenshots

### Admin — Dashboard
HR / interviewers manage interviews, questions, and the platform from a single back-office console.
![Admin Dashboard](./images/admin-dashboard.jpg)

### Admin — Create Interview
Pick a question, schedule a slot, and generate a one-click invitation link in seconds.
![Create Interview](./images/admin-create-interview.jpg)

### Admin — Invitation Link
Each interview produces a unique, expiring invitation URL for the candidate.
![Invitation Link](./images/admin-invitation-link.jpg)

### Candidate — Invitation Page
Candidates land on a clean invitation page that explains the rules before they join.
![Candidate Invitation](./images/candidate-invitation.jpg)

### Candidate — Interview Workspace
A full multi-file IDE with file tree, CodeMirror 6 editor, test-result panel, integrated terminal, and a side AI assistant with live model selection (Gemini, GPT, etc.).
![Interview Workspace](./images/interview-workspace.jpg)

---

## Features

### For candidates
- **Multi-file project IDE** built on CodeMirror 6 with file tree and tabs — not a single-file LeetCode box.
- **Governed AI assistant** with streaming responses (AI SDK v6 + Spring AI). The interviewer chooses which model is allowed per session.
- **Checkpoint-driven progression** — each question is split into stages (debug → implement → optimise) following Meta's three-phase format.
- **Real test execution** in a sandboxed Docker container, with live test output and an interactive terminal (xterm.js + WebSocket).
- **Auto-advancing flow**: pass a checkpoint → next stage unlocks → finish all → completion screen.

### For interviewers / HR
- **One-click interview creation** with question selection, scheduling and AI-model policy.
- **Invitation system** with unique tokens and configurable expiry.
- **Interview lifecycle management**: `SCHEDULED → ACTIVE → COMPLETED / CANCELLED`, with state transitions enforced at the domain level.
- **Conversation history & checkpoint results** are persisted for post-interview review.
- **Pilot AI scoring (preview)** — an LLM judge produces a structured agency score against Meta's evaluation rubric.

### For platform owners
- **Self-hostable**: one Docker Compose file brings up backend, two frontends, PostgreSQL and a sandboxed Docker-in-Docker for code execution.
- **Spring Modulith** modular monolith — clean module boundaries (`interview / execution / invitation / question / ai / scoring`) verified by `ModularityTests`, leaving the door open to split into services later.
- **Liquibase migrations**, **Spring Data JDBC** (no JPA magic), and **Testcontainers** + **Cucumber** BDD scenarios out of the box.

---

## Architecture

```
┌─────────────────────┐      ┌─────────────────────┐
│  Admin (Next.js)    │      │ Candidate (Next.js) │
│  port 3000          │      │ port 3001           │
└──────────┬──────────┘      └──────────┬──────────┘
           │   Route Handlers (proxy)   │
           └────────────┬───────────────┘
                        ▼
        ┌──────────────────────────────────┐
        │  Spring Boot 4.0 — Modulith      │
        │  ┌────────────────────────────┐  │
        │  │ interview / invitation /   │  │
        │  │ question / execution /     │  │
        │  │ ai / scoring               │  │
        │  └────────────────────────────┘  │
        └────┬──────────┬─────────┬────────┘
             ▼          ▼         ▼
       PostgreSQL    Docker     Gemini /
       17 + Liqui-   sandbox    Spring AI
       base          (DinD)     ChatClient
```

### Tech stack

| Layer            | Choice                                                              |
| ---------------- | ------------------------------------------------------------------- |
| Backend          | Spring Boot **4.0**, Java **25**, Spring Modulith, Spring Data JDBC |
| AI               | Spring AI **2.0.0-M4** (Gemini 3 Flash Preview by default)          |
| Database         | PostgreSQL **17** + Liquibase                                       |
| Code execution   | Docker (docker-java); roadmap: Firecracker microVM               |
| Frontend         | Next.js **16.1** (App Router), TypeScript, Tailwind CSS 4           |
| Editor           | CodeMirror **6** (`@uiw/react-codemirror`)                          |
| AI UI            | AI SDK v6 (`useChat` + `DefaultChatTransport`) + AI Elements        |
| Terminal         | xterm.js + WebSocket                                                |
| Monorepo         | npm Workspaces (`apps/admin`, `apps/candidate`, `packages/shared`)  |
| Testing          | JUnit 5, Mockito, Testcontainers, **Cucumber 7** BDD                |

---

## Project layout

```
ai-coding-interview/
├── backend/              # Spring Boot 4 / Java 25 (Gradle Kotlin DSL)
│   └── src/main/java/com/interview/
│       ├── interview/    # Interview aggregate, checkpoint progress
│       ├── invitation/   # Invitation tokens & candidate join flow
│       ├── question/     # Questions loaded from classpath YAML
│       ├── execution/    # Sandboxed Docker code execution
│       ├── ai/           # Spring AI ChatClient + streaming SSE
│       └── scoring/      # LLM judge for agency scoring (preview)
├── frontend/             # npm Workspaces monorepo
│   ├── apps/admin/       # Interviewer app (port 3000)
│   ├── apps/candidate/   # Candidate app (port 3001)
│   └── packages/shared/  # Shared types, API client, UI elements
├── docs/                 # PRD, design docs, research notes
├── images/               # README screenshots
├── backend/compose.yaml          # PostgreSQL for local backend dev
├── docker-compose.yml    # Local full-stack runtime
└── build-and-run.sh      # One-shot: build images and start everything
```

---

## Getting started

Read the **[local developer setup guide](docs/getting-started-local.md)** for API keys, building `exams/100`, loading it into DinD, deployment and validation. Use JDK 25; frontend development uses Node 24/npm.

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


---

## Testing

```bash
cd backend

./gradlew test                                              # everything (needs Docker)
./gradlew test --tests "com.interview.*.bdd.*"              # Cucumber BDD scenarios
./gradlew test --tests "com.interview.ModularityTests"      # module boundary checks
```

---

## Roadmap

Implemented today (MVP):

- ✅ Interview lifecycle, invitations, multi-file Checkpoints
- ✅ Sandboxed Docker code execution + interactive terminal
- ✅ Streaming AI chat with dynamic model selection
- ✅ Pilot LLM-as-judge agency scoring

Next up:

- 🔮 Real-time interviewer "follow the candidate" mode + AI-interaction live panel
- 🔮 Full session replay (keystroke-level, multi-track, variable speed)
- 🔮 Question authoring UI + AI-assisted difficulty calibration
- 🔮 Anti-cheat signals (tab-switch, external paste, behavioural analysis)
- 🔮 Firecracker microVM execution backend

See [`docs/PRD.md`](./docs/PRD.md) for the full product spec.

---

## Contributing

Issues and pull requests are very welcome. Before opening a PR:

1. Run `./gradlew test` and make sure the Modulith verification still passes.
2. New backend behaviour should come with a Cucumber feature in `backend/src/test/resources/features/`.
3. Follow the coding principles in [`CLAUDE.md`](./CLAUDE.md): readability first, expressive names, comments explain *why*, stateless services.

---

## License

Apache License 2.0 — see [`LICENSE`](./LICENSE).
