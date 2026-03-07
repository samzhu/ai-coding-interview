"use client";

import { useChat } from "@ai-sdk/react";
import { DefaultChatTransport } from "ai";
import type { UIMessage } from "ai";
import { useMemo, useEffect, useRef, useState } from "react";
import { CheckCircle, XCircle, Loader2, FileSearch, FolderOpen, Terminal } from "lucide-react";
import { ThinkingAnimation } from "./thinking-animation";
import type { AiModelInfo, ChatMessage } from "@interview/shared/types";
import { apiGet, getApiUrl } from "@interview/shared/lib/api-client";
import { useInterview } from "@/contexts/interview-context";
import {
  Conversation,
  ConversationContent,
  ConversationEmptyState,
  ConversationScrollButton,
} from "@interview/shared/components/ai-elements/conversation";
import {
  Message,
  MessageContent,
  MessageResponse,
} from "@interview/shared/components/ai-elements/message";
import {
  PromptInput,
  PromptInputFooter,
  PromptInputSelect,
  PromptInputSelectContent,
  PromptInputSelectItem,
  PromptInputSelectTrigger,
  PromptInputSelectValue,
  PromptInputSubmit,
  PromptInputTextarea,
} from "@interview/shared/components/ai-elements/prompt-input";
import { ChangeSetCard } from "./changeset-card";
import type { EditProposalData } from "@/lib/changeset";

interface AiChatPanelProps {
  interviewId: string;
  aiEnabled?: boolean;
}

// edit_proposal XML 區塊的正則（後端串流時包含原始 XML，前端需過濾避免顯示生硬標籤）
const EDIT_PROPOSAL_REGEX = /<edit_proposal[\s\S]*?<\/edit_proposal>/g;

function getTextContent(message: UIMessage): string {
  const raw = message.parts
    .filter((p): p is Extract<typeof p, { type: "text" }> => p.type === "text")
    .map((p) => p.text)
    .join("");
  return raw.replace(EDIT_PROPOSAL_REGEX, "").trim();
}

// AI SDK v6 要求 data 事件 type 以 "data-" 開頭，
// 後端送出 {"type":"data-edit-proposal",...}，
// SDK 會在 message.parts 加入 { type: "data-edit-proposal", id, data: {...} }。
function getEditProposals(message: UIMessage): EditProposalData[] {
  return message.parts
    .filter(
      (p): p is Extract<typeof p, { type: `data-${string}` }> =>
        p.type === "data-edit-proposal"
    )
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    .map((p) => (p as any).data)
    .filter(
      (item: unknown): item is EditProposalData =>
        typeof item === "object" &&
        item !== null &&
        (item as EditProposalData).type === "edit-proposal"
    );
}

/**
 * Tool 調用事件型別（對應後端 data-tool-invocation SSE 事件）。
 *
 * 設計說明：後端在每個 tool 執行前後透過 toolSseEmitter 推送事件。
 * 同一 toolCallId 可能有兩個事件：running（開始）→ completed/error（結束）。
 * 前端以 toolCallId 為 key 去重，取最新狀態渲染。
 */
interface ToolInvocationData {
  toolCallId: string;
  toolName: "listFiles" | "readFile" | "runCommand";
  state: "running" | "completed" | "error";
  summary?: string;
}

/**
 * 從 message.parts 提取 tool invocation 事件，按 toolCallId 去重（取最新狀態）。
 * 回傳 Map<toolCallId, ToolInvocationData>，保留插入順序（LinkedHashMap 語義）。
 */
function getToolInvocations(message: UIMessage): Map<string, ToolInvocationData> {
  const result = new Map<string, ToolInvocationData>();
  for (const p of message.parts) {
    if (p.type !== "data-tool-invocation") continue;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const data = (p as any).data as ToolInvocationData;
    if (data?.toolCallId && data?.toolName && data?.state) {
      // 相同 toolCallId 後來的事件（completed/error）覆蓋先前的（running）
      result.set(data.toolCallId, data);
    }
  }
  return result;
}

/** Tool 名稱對應中文顯示名與圖示 */
const TOOL_META: Record<string, { label: string; Icon: React.ComponentType<{ className?: string }> }> = {
  listFiles: { label: "列出工作區檔案", Icon: FolderOpen },
  readFile:  { label: "讀取檔案", Icon: FileSearch },
  runCommand:{ label: "執行指令", Icon: Terminal },
};

/**
 * ToolInvocationBadge — 顯示單個 tool 調用的狀態徽章。
 *
 * 設計說明：
 * - running：旋轉動畫 + 淡藍色，表示工具執行中（可能持續數秒）
 * - completed：綠色勾選，表示工具執行完成
 * - error：紅色叉，表示工具執行失敗
 * 徽章寬度適合聊天面板（~320px），過長的 summary 截斷顯示。
 */
function ToolInvocationBadge({ toolName, state, summary }: ToolInvocationData) {
  const meta = TOOL_META[toolName] ?? { label: toolName, Icon: Terminal };
  const { Icon } = meta;

  const stateConfig = {
    running: {
      containerCls: "border-[#264f78] bg-[#1a2d3d]",
      textCls: "text-[#9cdcfe]",
      icon: <Loader2 className="w-3 h-3 text-[#9cdcfe] animate-spin shrink-0" />,
    },
    completed: {
      containerCls: "border-[#1e3a1e] bg-[#1a2e1a]",
      textCls: "text-[#4ec9b0]",
      icon: <CheckCircle className="w-3 h-3 text-[#4ec9b0] shrink-0" />,
    },
    error: {
      containerCls: "border-[#4d1d1d] bg-[#2e1a1a]",
      textCls: "text-[#f48771]",
      icon: <XCircle className="w-3 h-3 text-[#f48771] shrink-0" />,
    },
  }[state];

  return (
    <div
      className={`w-full flex items-center gap-1.5 px-2 py-1 rounded border text-[10px] font-mono ${stateConfig.containerCls}`}
    >
      {stateConfig.icon}
      <Icon className={`w-3 h-3 shrink-0 ${stateConfig.textCls}`} />
      <span className={`${stateConfig.textCls} truncate`}>
        {meta.label}
        {summary && <span className="opacity-70 ml-1">{summary}</span>}
      </span>
    </div>
  );
}

export function AiChatPanel({ interviewId, aiEnabled = true }: AiChatPanelProps) {
  const { state, registerSendAiMessage, registerChangeSet, rejectChangeSet } = useInterview();
  const [models, setModels] = useState<AiModelInfo[]>([]);
  const [selectedModelId, setSelectedModelId] = useState("");
  // 用 ref 存模型 ID，讓 transport body function 每次 request 動態取值，
  // 而不需要重建 transport（重建會清空 messages）
  const modelIdRef = useRef(selectedModelId);
  modelIdRef.current = selectedModelId;

  const transport = useMemo(
    () =>
      new DefaultChatTransport({
        api: getApiUrl(`/interviews/${interviewId}/ai/chat/stream`),
        body: () => ({ modelId: modelIdRef.current }),
      }),
    [interviewId]
  );

  const { messages, setMessages, sendMessage, status, error, stop } = useChat({ transport });

  // 判斷 AI 是否正在思考（顯示乳牛動畫）：
  // - submitted：已送出但伺服器尚未回應任何 SSE 事件
  // - streaming 且尚無文字：AI 正在執行工具或生成回覆（可能持續 5-30 秒）
  // 動畫與 ToolInvocationBadge 同時顯示，提供雙重視覺回饋：
  //   工具卡片 = 具體進度（正在做什麼），動畫 = 整體狀態（AI 仍在處理中）。
  // 一旦文字串流開始（lastHasText = true），動畫消失，文字接手。
  const lastMsg = messages.at(-1);
  const lastIsAssistant = lastMsg?.role === "assistant";
  const lastHasText = lastIsAssistant && getTextContent(lastMsg!) !== "";
  const lastHasToolEvents = lastIsAssistant && getToolInvocations(lastMsg!).size > 0;
  const isThinking =
    status === "submitted" ||
    (status === "streaming" && lastIsAssistant && !lastHasText);

  // 思考中計時器，重置條件為 isThinking 從 true 轉 false
  const [thinkingSeconds, setThinkingSeconds] = useState(0);
  useEffect(() => {
    if (!isThinking) {
      setThinkingSeconds(0);
      return;
    }
    const timer = setInterval(() => setThinkingSeconds((s) => s + 1), 1000);
    return () => clearInterval(timer);
  }, [isThinking]);

  // 將 sendMessage 包裝後註冊到 context，讓其他元件可送出 AI 訊息
  useEffect(() => {
    registerSendAiMessage((text: string) => {
      sendMessage({ text });
    });
  }, [registerSendAiMessage, sendMessage]);

  // 錯誤發生時輸出完整細節到 console，方便開發除錯
  useEffect(() => {
    if (!error) return;
    console.error("[AiChatPanel] useChat error:", error);
    // 嘗試印出 cause（AI SDK 有時把 HTTP response body 放在 cause）
    if ((error as Error & { cause?: unknown }).cause) {
      console.error("[AiChatPanel] error.cause:", (error as Error & { cause?: unknown }).cause);
    }
  }, [error]);

  useEffect(() => {
    apiGet<AiModelInfo[]>("/ai/models")
      .then((list) => {
        setModels(list);
        if (list.length > 0) setSelectedModelId(list[0].id);
      })
      .catch((err) => {
        console.error("[AiChatPanel] Failed to load AI models:", err);
      });
  }, []);

  useEffect(() => {
    async function loadHistory() {
      const res = await fetch(getApiUrl(`/interviews/${interviewId}/ai/history`));
      if (!res.ok) return;
      const data: { messages: ChatMessage[] } = await res.json();
      const history: UIMessage[] = data.messages
        .filter((m) => m.role !== "SYSTEM")
        .map((m) => ({
          id: m.id,
          role: m.role.toLowerCase() as "user" | "assistant",
          parts: [{ type: "text" as const, text: m.content }],
          createdAt: new Date(m.createdAt),
        }));
      if (history.length > 0) {
        setMessages(history);
      }
    }
    loadHistory();
  }, [interviewId, setMessages]);

  // 當 AI 回應完成（streaming → ready）且含有 proposals，自動 REGISTER_CHANGESET。
  // 使用 ref 追蹤已處理的 messageId，避免重複 dispatch。
  // 歷史訊息（loadHistory 載入的）只有 text parts，不含 data-edit-proposal，
  // 所以不會被誤觸發，可安全處理所有 messages。
  const registeredMessageIds = useRef(new Set<string>());
  useEffect(() => {
    if (status === "streaming" || status === "submitted") return;

    for (const message of messages) {
      if (message.role !== "assistant") continue;
      if (registeredMessageIds.current.has(message.id)) continue;

      const proposals = getEditProposals(message);
      registeredMessageIds.current.add(message.id);

      if (proposals.length > 0) {
        registerChangeSet(message.id, proposals);
      }
    }
  }, [messages, status, registerChangeSet]);

  return (
    <div className="flex flex-col h-full bg-[#1e1e1e]">
      {!aiEnabled ? (
        <div className="flex flex-1 items-center justify-center p-6 text-center">
          <div>
            <p className="text-sm font-medium text-[#858585] mb-1">此階段 AI 不可用</p>
            <p className="text-xs text-[#585858]">
              本階段考驗您獨立解題能力，請不要依賴 AI 提示
            </p>
          </div>
        </div>
      ) : (
        <>
          <Conversation className="flex-1">
            <ConversationContent className="p-3">
              {messages.length === 0 && (
                <ConversationEmptyState
                  title="準備好了"
                  description="您可以詢問 AI 助理關於題目的提示或說明"
                />
              )}
              {messages.map((message) => {
                const textContent = getTextContent(message);
                const proposals =
                  message.role === "assistant" ? getEditProposals(message) : [];
                const toolInvocations =
                  message.role === "assistant" ? getToolInvocations(message) : new Map();

                // streaming 中的空 assistant message（無文字且無 tool 事件）
                // 由 ThinkingAnimation 取代顯示，避免空泡泡
                if (
                  message.role === "assistant" &&
                  !textContent &&
                  !proposals.length &&
                  toolInvocations.size === 0 &&
                  isThinking
                ) {
                  return null;
                }

                return (
                  <Message key={message.id} from={message.role}>
                    <MessageContent>
                      {message.role === "assistant" ? (
                        <>
                          {/* Tool invocation 狀態徽章：顯示於文字回應之前 */}
                          {toolInvocations.size > 0 && (
                            <div className="flex flex-col gap-1 mb-2">
                              {Array.from(toolInvocations.values()).map((tool) => (
                                <ToolInvocationBadge key={tool.toolCallId} {...tool} />
                              ))}
                            </div>
                          )}
                          {textContent && (
                            <MessageResponse>{textContent}</MessageResponse>
                          )}
                          {proposals.length > 0 && (
                            <ChangeSetCard
                              interviewId={interviewId}
                              messageId={message.id}
                              proposals={proposals}
                            />
                          )}
                        </>
                      ) : (
                        <p className="whitespace-pre-wrap text-sm">{textContent}</p>
                      )}
                    </MessageContent>
                  </Message>
                );
              })}
              {isThinking && <ThinkingAnimation thinkingSeconds={thinkingSeconds} />}
            </ConversationContent>
            <ConversationScrollButton />
          </Conversation>

          {error && (
            <details className="mx-3 mb-2 rounded border border-red-900/50 bg-red-950/30 text-xs shrink-0 open:pb-2">
              <summary className="cursor-pointer px-3 py-1.5 text-red-400 select-none list-none flex items-center gap-1.5">
                <span className="text-red-500">⚠</span>
                <span>{error.message || "AI 回應發生錯誤"}</span>
                <span className="ml-auto text-red-600 text-[10px]">展開詳情 ▾</span>
              </summary>
              <pre className="mx-3 mt-1 text-[10px] text-red-300 whitespace-pre-wrap break-all leading-relaxed max-h-40 overflow-y-auto">
                {[
                  error.name && `[${error.name}]`,
                  error.message,
                  (error as Error & { cause?: unknown }).cause != null &&
                    `cause: ${JSON.stringify((error as Error & { cause?: unknown }).cause, null, 2)}`,
                  error.stack,
                ]
                  .filter(Boolean)
                  .join("\n")}
              </pre>
            </details>
          )}

          <div className="p-3 border-t border-[#333] shrink-0">
            <PromptInput
              onSubmit={(msg) => {
                // 送出新訊息前，隱性拒絕當前 pending 的 ChangeSet（如有）
                if (state.pendingChangeSetId) {
                  rejectChangeSet(state.pendingChangeSetId);
                }
                sendMessage({ text: msg.text ?? "" });
              }}
            >
              <PromptInputTextarea placeholder="輸入問題...（Enter 送出，Shift+Enter 換行）" />
              <PromptInputFooter>
                {models.length > 1 ? (
                  <PromptInputSelect value={selectedModelId} onValueChange={setSelectedModelId}>
                    {/* 深色 pill 風格，在 #1e1e1e 背景上清晰可見 */}
                    <PromptInputSelectTrigger className="h-7 w-auto max-w-[200px] text-xs bg-[#2d2d2d] border border-[#404040] rounded-md text-[#cccccc] hover:bg-[#353535] hover:text-[#e0e0e0]">
                      <PromptInputSelectValue />
                    </PromptInputSelectTrigger>
                    <PromptInputSelectContent>
                      {models.map((m) => (
                        <PromptInputSelectItem key={m.id} value={m.id}>
                          {m.name}
                        </PromptInputSelectItem>
                      ))}
                    </PromptInputSelectContent>
                  </PromptInputSelect>
                ) : models.length === 1 ? (
                  // 單一模型時顯示純文字 pill，讓使用者知道當前模型
                  <span className="text-xs text-[#999] bg-[#2d2d2d] border border-[#404040] rounded-md px-2.5 py-1 select-none">
                    {models[0].name}
                  </span>
                ) : (
                  <div />
                )}
                <PromptInputSubmit status={status} onStop={stop} />
              </PromptInputFooter>
            </PromptInput>
          </div>
        </>
      )}
    </div>
  );
}
