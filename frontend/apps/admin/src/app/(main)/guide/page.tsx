import { CalloutBox } from "@/components/guide/callout-box";
import { CodeBlock } from "@/components/guide/code-block";
import { ConstraintTable } from "@/components/guide/constraint-table";
import { SchemaField } from "@/components/guide/schema-field";
import { StepSection } from "@/components/guide/step-section";

export default function GuidePage() {
  return (
    <div className="max-w-3xl mx-auto pb-16">
      {/* 頁首 */}
      <div className="mb-10">
        <h1 className="text-3xl font-bold mb-2">如何建置考試映像檔</h1>
        <p className="text-muted-foreground text-lg">
          完整指南：從專案目錄到 Docker Image，一步步建立面試題目
        </p>
      </div>

      {/* Overview */}
      <div className="mb-12">
        <h2 className="text-lg font-semibold mb-3">平台運作流程</h2>
        <ol className="space-y-1 text-sm text-muted-foreground list-decimal list-inside">
          <li>面試官準備 Docker image，推送至 Docker Hub 或私有 Registry</li>
          <li>在後台建立題目，填入 image 名稱與 question.yaml</li>
          <li>平台拉取 image、啟動隔離容器</li>
          <li>
            平台讀取容器內的{" "}
            <code className="font-mono bg-muted px-1 rounded">
              /workspace/exam.yml
            </code>{" "}
            取得 checkpoints
          </li>
          <li>候選人依序完成每個 checkpoint 的 testCommand</li>
        </ol>
        <CalloutBox variant="important">
          <code className="font-mono bg-primary/10 px-1 rounded">exam.yml</code>{" "}
          是映像檔與平台之間的合約。沒有這個檔案，平台無法載入任何 checkpoint。
        </CalloutBox>
      </div>

      {/* Step 1 */}
      <StepSection step={1} title="建立專案目錄結構">
        <p className="text-sm text-muted-foreground mb-3">
          建議將題目的所有檔案放在一個獨立目錄，<code className="font-mono bg-muted px-1 rounded">exam/</code>{" "}
          子目錄的內容會被完整複製到容器的{" "}
          <code className="font-mono bg-muted px-1 rounded">/workspace</code>。
        </p>
        <CodeBlock language="bash">
          {`question01/
├── Dockerfile          # 定義映像檔建置流程
└── exam/               # 此目錄 → 容器 /workspace
    ├── exam.yml        # 平台讀取的合約檔（必要）
    ├── src/            # 題目程式碼
    │   └── main/
    │       └── java/
    ├── tests/          # 測試檔案（readonly）
    ├── build.gradle    # 或 pom.xml / requirements.txt
    └── gradlew         # 建置腳本（如需要）`}
        </CodeBlock>
        <CalloutBox variant="tip">
          <code className="font-mono bg-primary/10 px-1 rounded">exam/</code>{" "}
          目錄的內容會成為容器內的{" "}
          <code className="font-mono bg-primary/10 px-1 rounded">/workspace</code>。
          Dockerfile 中用 <code className="font-mono bg-primary/10 px-1 rounded">COPY exam/ .</code> 複製。
        </CalloutBox>
      </StepSection>

      {/* Step 2 */}
      <StepSection step={2} title="撰寫 exam.yml">
        <p className="text-sm text-muted-foreground mb-3">
          這是平台讀取 checkpoints 的唯一來源。放在{" "}
          <code className="font-mono bg-muted px-1 rounded">exam/exam.yml</code>，
          容器啟動後自動位於{" "}
          <code className="font-mono bg-muted px-1 rounded">/workspace/exam.yml</code>。
        </p>
        <CodeBlock
          title="exam/exam.yml"
          language="yaml"
          annotations={{
            3: "排除不顯示給 candidate 的檔案",
            11: "第一個 checkpoint",
            12: "顯示在 UI 的標題",
            13: "顯示在 UI 的說明（Markdown）",
            17: "平台執行的測試指令",
            19: "第二個 checkpoint",
          }}
        >
          {`# 排除規則：.gitignore 風格 glob，不顯示給 candidate
exclude:
  - ".gradle/"
  - "build/"
  - "bin/"
  - "*.class"
  - "*.jar"

# 關卡定義（按順序執行）
checkpoints:
  - id: "1"
    title: "Bug Fix — 修復遊戲邏輯"
    description: |
      QA 回報了兩個 Bug：
      1. 重複猜同一個字母會多次扣命
      2. isGameWon() 回傳了錯誤的條件
      請閱讀程式碼找出並修復這兩個問題。
    testCommand: "./gradlew test --tests 'exam.question.bdd.CP1Test' --no-daemon"

  - id: "2"
    title: "Feature — 實作提示系統"
    description: |
      實作 HintProvider.java 中的 3 個 TODO 方法。
    testCommand: "./gradlew test --tests 'exam.question.bdd.CP2Test' --no-daemon"`}
        </CodeBlock>

        {/* Schema Reference */}
        <h3 className="text-base font-semibold mt-6 mb-3">Schema Reference</h3>
        <div className="rounded-lg border border-border px-4">
          <SchemaField name="exclude" type="string[]" required={false}>
            .gitignore 風格 glob，符合的路徑不顯示給 candidate（exam.yml 本身自動排除）
          </SchemaField>
          <SchemaField name="checkpoints" type="Checkpoint[]" required={true}>
            關卡陣列，依序執行；至少需要 1 個
          </SchemaField>
          <SchemaField name="checkpoints[].id" type="string" required={true}>
            關卡唯一識別碼（建議用 &quot;1&quot;, &quot;2&quot; 依序遞增）
          </SchemaField>
          <SchemaField name="checkpoints[].title" type="string" required={true}>
            顯示在 UI 的標題
          </SchemaField>
          <SchemaField name="checkpoints[].description" type="string" required={true}>
            顯示在 UI 的說明，支援多行文字（用 YAML <code className="font-mono bg-muted px-1 rounded">|</code> 區塊）
          </SchemaField>
          <SchemaField name="checkpoints[].testCommand" type="string" required={true}>
            平台在容器內執行的測試指令，透過 <code className="font-mono bg-muted px-1 rounded">bash -c</code> 呼叫
          </SchemaField>
        </div>

        <CalloutBox variant="tip">
          雖然容器有網路，仍建議在 <strong>Docker build 階段</strong>預先載入所有依賴，
          避免 <code className="font-mono bg-emerald-500/10 px-1 rounded">testCommand</code>{" "}
          執行時即時下載，拖慢測試速度。
        </CalloutBox>
      </StepSection>

      {/* Step 3 */}
      <StepSection step={3} title="撰寫 Dockerfile">
        <p className="text-sm text-muted-foreground mb-3">
          Dockerfile 的核心職責：選擇正確的 runtime、複製題目檔案、
          <strong>在有網路的 build 階段預熱所有依賴</strong>。
        </p>

        {/* Java/Gradle 範例 */}
        <h3 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider mb-2">
          Java / Gradle
        </h3>
        <CodeBlock
          title="Dockerfile"
          language="dockerfile"
          annotations={{
            1: "選用含 JDK 的 image",
            3: "工作目錄 = /workspace",
            5: "複製 exam/ 到容器",
            7: "預熱：下載依賴 + 編譯（有網路）",
            10: "保持容器存活",
          }}
        >
          {`FROM eclipse-temurin:25-jdk

WORKDIR /workspace

COPY exam/ .

RUN chmod +x ./gradlew && \\
    ./gradlew testClasses --no-daemon

CMD ["tail", "-f", "/dev/null"]`}
        </CodeBlock>

        {/* Python 範例 */}
        <h3 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider mt-6 mb-2">
          Python
        </h3>
        <CodeBlock title="Dockerfile" language="dockerfile">
          {`FROM python:3.12-slim

WORKDIR /workspace

COPY exam/ .

RUN pip install --no-cache-dir -r requirements.txt

CMD ["tail", "-f", "/dev/null"]`}
        </CodeBlock>

        {/* Node.js 範例 */}
        <h3 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider mt-6 mb-2">
          Node.js
        </h3>
        <CodeBlock title="Dockerfile" language="dockerfile">
          {`FROM node:22-slim

WORKDIR /workspace

COPY exam/ .

RUN npm ci

CMD ["tail", "-f", "/dev/null"]`}
        </CodeBlock>

        <CalloutBox variant="tip">
          <strong>建議預熱依賴</strong>：在 <code className="font-mono bg-emerald-500/10 px-1 rounded">RUN</code>{" "}
          步驟預先下載所有依賴並編譯，可大幅加速{" "}
          <code className="font-mono bg-emerald-500/10 px-1 rounded">testCommand</code>{" "}
          執行。雖然容器有網路，即時下載仍會拖慢測試速度。
        </CalloutBox>
      </StepSection>

      {/* Step 4 */}
      <StepSection step={4} title="本機建置與測試">
        <p className="text-sm text-muted-foreground mb-3">
          推送前先在本機驗證映像檔，確認 testCommand 可正常執行。
        </p>
        <CodeBlock language="bash">
          {`# 建置映像檔
docker build -t my-org/question01:latest question01/

# 模擬平台執行環境（互動式 shell）
docker run --rm -it my-org/question01:latest bash

# 容器內驗證 testCommand（應全部通過）
./gradlew test --tests 'exam.question.bdd.CP1Test' --no-daemon`}
        </CodeBlock>
        <CalloutBox variant="tip">
          互動式進入容器執行 testCommand，可快速確認 build 階段的預熱是否完整。
          若依賴仍需即時下載，建議回到 Dockerfile 的 <code className="font-mono bg-emerald-500/10 px-1 rounded">RUN</code>{" "}
          步驟補強預熱邏輯。
        </CalloutBox>
      </StepSection>

      {/* Step 5 */}
      <StepSection step={5} title="推送與註冊題目">
        <p className="text-sm text-muted-foreground mb-3">
          映像檔驗證通過後，推送至 Registry，再於後台建立題目記錄。
        </p>
        <CodeBlock language="bash">
          {`# 推送至 Docker Hub
docker push my-org/question01:latest

# 推送至私有 Registry
docker tag my-org/question01:latest registry.example.com/question01:latest
docker push registry.example.com/question01:latest`}
        </CodeBlock>

        <p className="text-sm text-muted-foreground mt-4 mb-3">
          在 <code className="font-mono bg-muted px-1 rounded">backend/src/main/resources/questions/</code>{" "}
          新增題目 YAML，平台啟動時自動載入：
        </p>
        <CodeBlock
          title="backend/src/main/resources/questions/question01.yaml"
          language="yaml"
          annotations={{
            1: "唯一識別碼",
            5: "必須與 docker push 的 tag 完全一致",
          }}
        >
          {`id: "question1"
title: "Java Hangman 猜字遊戲"
language: "java"
difficulty: "MEDIUM"
image: "my-org/question01:latest"
description: |
  你接手了一個 Java Hangman 猜字遊戲專案。
  前一位工程師留下了有 Bug 的程式碼和未實作的功能。`}
        </CodeBlock>

        <CalloutBox variant="important">
          <code className="font-mono bg-sky-500/10 px-1 rounded">image</code>{" "}
          欄位必須與 <code className="font-mono bg-sky-500/10 px-1 rounded">docker push</code>{" "}
          使用的 tag 完全一致，包含 Registry host（如有）。
          平台會原封不動地用這個字串呼叫 <code className="font-mono bg-sky-500/10 px-1 rounded">docker pull</code>。
        </CalloutBox>
      </StepSection>

      {/* Quick Reference */}
      <div className="mt-4">
        <h2 className="text-xl font-semibold mb-4">Quick Reference</h2>
        <h3 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider mb-3">
          平台限制
        </h3>
        <ConstraintTable />

        <CalloutBox variant="warning" title="常見錯誤">
          <ul className="space-y-1 list-disc list-inside">
            <li>
              <strong>testCommand 失敗</strong>：依賴未在 build 階段預熱 —
              加強 <code className="font-mono bg-amber-500/10 px-1 rounded">RUN</code> 步驟確保所有套件已下載
            </li>
            <li>
              <strong>exam.yml 找不到</strong>：檔案路徑錯誤 — 必須位於容器的{" "}
              <code className="font-mono bg-amber-500/10 px-1 rounded">/workspace/exam.yml</code>
            </li>
            <li>
              <strong>容器立即退出</strong>：缺少 <code className="font-mono bg-amber-500/10 px-1 rounded">CMD ["tail", "-f", "/dev/null"]</code>{" "}
              — 平台需要容器持續存活
            </li>
            <li>
              <strong>image 拉取失敗</strong>：Registry 需設為 public，
              或在平台設定 imagePullSecrets
            </li>
          </ul>
        </CalloutBox>
      </div>
    </div>
  );
}
