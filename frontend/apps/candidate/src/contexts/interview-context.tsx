"use client";

import {
  createContext,
  useContext,
  useReducer,
  useCallback,
  useRef,
  type ReactNode,
} from "react";
import type {
  CheckpointResultResponse,
  CheckpointFileState,
  InterviewStatus,
  ExecutionResponse,
} from "@interview/shared/types";
import {
  groupProposalsByFile,
  type ChangeSet,
  type EditProposalData,
} from "@/lib/changeset";
import { mergeProposals } from "@/lib/diff-utils";

/**
 * DiffReviewState — 多檔案 diff 審查狀態
 *
 * 設計說明：同一 AI 回應可能對多個檔案提案修改，
 * fileDiffs 以 filePath 為 key 儲存每個檔案的原始與提案完整內容。
 * 編輯器根據 state.activeFilePath 決定顯示哪個檔案的 diff，
 * 不需要額外的 activeReviewFilePath 欄位（複用既有的 activeFilePath）。
 */
interface DiffReviewState {
  changeSetMessageId: string;
  fileDiffs: Map<string, { originalFullContent: string; proposedFullContent: string }>;
}

interface InterviewState {
  interviewId: string;
  status: InterviewStatus;
  // 目前「active」的 checkpoint（使用者正在操作的），隨 switchCheckpoint 切換
  checkpoint: CheckpointResultResponse | null;
  // 所有 checkpoint 的完整清單，保留 status 供 stepper 顯示與自由跳選
  checkpoints: CheckpointResultResponse[];
  aiEnabled: boolean;
  files: Map<string, CheckpointFileState>;
  openFiles: string[];
  activeFilePath: string | null;
  code: string;
  isSubmitting: boolean;
  isRunning: boolean;
  lastExecution: ExecutionResponse | null;
  // VSCode 風格 inline diff 審查狀態，null 表示未進入 diff 模式
  diffReview: DiffReviewState | null;
  // 所有 ChangeSets（key = messageId），供 ChangeSetCard 讀取狀態
  changeSets: Map<string, ChangeSet>;
  // 目前 pending（等待使用者決定）的 ChangeSet messageId，null 表示無
  pendingChangeSetId: string | null;
}

type InterviewAction =
  | { type: "SET_CHECKPOINT"; payload: CheckpointResultResponse }
  | { type: "SWITCH_CHECKPOINT"; payload: string } // payload = checkpointId
  | { type: "SET_CODE"; payload: string }
  | { type: "SET_FILE_CONTENT"; payload: { filePath: string; content: string } }
  | { type: "SET_ACTIVE_FILE"; payload: string }
  | { type: "OPEN_FILE"; payload: string }
  | { type: "CLOSE_FILE"; payload: string }
  | { type: "SET_SUBMITTING"; payload: boolean }
  | { type: "SET_RUNNING"; payload: boolean }
  | { type: "SET_EXECUTION"; payload: ExecutionResponse }
  | { type: "SET_STATUS"; payload: InterviewStatus }
  | { type: "LOAD_FILES"; payload: Map<string, CheckpointFileState> }
  | { type: "REGISTER_CHANGESET"; payload: { messageId: string; proposals: EditProposalData[] } }
  | { type: "APPLY_CHANGESET"; payload: { messageId: string } }
  | { type: "REJECT_CHANGESET"; payload: { messageId: string } }
  | {
      type: "CHECKPOINT_RESULT";
      payload: { result: CheckpointResultResponse; execution: ExecutionResponse | null };
    };

/**
 * 正規化查找：嘗試多種路徑格式匹配 state.files 的 key。
 * 設計說明：AI 看到 listFiles 回傳的絕對路徑（/workspace/main.py），
 * 但 system prompt 範例為相對路徑，AI 輸出格式不確定。
 * 依序嘗試：精確匹配 → 加 /workspace/ 前綴 → endsWith 後綴匹配。
 */
function resolveFilePath(files: Map<string, unknown>, proposalPath: string): string | null {
  if (files.has(proposalPath)) return proposalPath;
  if (!proposalPath.startsWith("/")) {
    const abs = "/workspace/" + proposalPath;
    if (files.has(abs)) return abs;
  }
  for (const key of files.keys()) {
    if (key.endsWith("/" + proposalPath)) return key;
  }
  return null;
}

function findReadme(files: Map<string, CheckpointFileState>): string | null {
  for (const filePath of files.keys()) {
    const name = filePath.split("/").pop() ?? "";
    if (name.toLowerCase() === "readme.md") return filePath;
  }
  return null;
}

function reducer(state: InterviewState, action: InterviewAction): InterviewState {
  switch (action.type) {
    case "SET_CHECKPOINT": {
      return {
        ...state,
        checkpoint: action.payload,
        aiEnabled: action.payload.aiEnabled ?? true,
        lastExecution: null,
      };
    }
    case "SWITCH_CHECKPOINT": {
      // 從 checkpoints[] 找到目標，切換為 active checkpoint
      const cp = state.checkpoints.find(c => c.checkpointId === action.payload);
      if (!cp) return state;
      return {
        ...state,
        checkpoint: cp,
        aiEnabled: cp.aiEnabled ?? true,
        lastExecution: null,
      };
    }
    case "LOAD_FILES": {
      const files = action.payload;
      const readmePath = findReadme(files);
      const openFiles = readmePath ? [readmePath] : [];
      const activeFilePath = readmePath;
      return {
        ...state,
        files,
        openFiles,
        activeFilePath,
        code: activeFilePath ? (files.get(activeFilePath)?.content ?? "") : "",
      };
    }
    case "SET_CODE":
      return {
        ...state,
        code: action.payload,
        files: state.activeFilePath
          ? new Map(state.files).set(state.activeFilePath, {
              ...state.files.get(state.activeFilePath)!,
              content: action.payload,
            })
          : state.files,
      };
    case "SET_FILE_CONTENT": {
      const { filePath, content } = action.payload;
      const newFiles = new Map(state.files);
      const existing = newFiles.get(filePath);
      if (existing) {
        newFiles.set(filePath, { ...existing, content });
      }
      return {
        ...state,
        files: newFiles,
        code: filePath === state.activeFilePath ? content : state.code,
      };
    }
    case "SET_ACTIVE_FILE": {
      const filePath = action.payload;
      const file = state.files.get(filePath);
      const openFiles = state.openFiles.includes(filePath)
        ? state.openFiles
        : [...state.openFiles, filePath];
      return {
        ...state,
        openFiles,
        activeFilePath: filePath,
        code: file?.content ?? state.code,
      };
    }
    case "OPEN_FILE": {
      const filePath = action.payload;
      const file = state.files.get(filePath);
      const openFiles = state.openFiles.includes(filePath)
        ? state.openFiles
        : [...state.openFiles, filePath];
      return {
        ...state,
        openFiles,
        activeFilePath: filePath,
        code: file?.content ?? state.code,
      };
    }
    case "CLOSE_FILE": {
      const path = action.payload;
      const newOpenFiles = state.openFiles.filter((f) => f !== path);
      let newActive = state.activeFilePath;
      if (state.activeFilePath === path) {
        if (newOpenFiles.length > 0) {
          const idx = state.openFiles.indexOf(path);
          newActive = newOpenFiles[Math.max(0, idx - 1)];
        } else {
          newActive = null;
        }
      }
      return {
        ...state,
        openFiles: newOpenFiles,
        activeFilePath: newActive,
        code: newActive ? (state.files.get(newActive)?.content ?? "") : "",
      };
    }
    case "SET_SUBMITTING":
      return { ...state, isSubmitting: action.payload };
    case "SET_RUNNING":
      return { ...state, isRunning: action.payload };
    case "SET_EXECUTION":
      return { ...state, lastExecution: action.payload };
    case "SET_STATUS":
      return { ...state, status: action.payload };

    case "REGISTER_CHANGESET": {
      const { messageId, proposals } = action.payload;

      // 若有上一個 pending changeset，先隱性拒絕它（還原檔案至原始內容）
      let currentFiles = new Map(state.files);
      let currentCode = state.code;
      const updatedChangeSets = new Map(state.changeSets);

      if (state.pendingChangeSetId && state.diffReview) {
        const prevId = state.pendingChangeSetId;
        const prevCs = updatedChangeSets.get(prevId);
        if (prevCs) updatedChangeSets.set(prevId, { ...prevCs, status: "rejected" });
        // 還原上一個 changeset 影響的檔案
        for (const [filePath, fileDiff] of state.diffReview.fileDiffs) {
          const file = currentFiles.get(filePath);
          if (file) {
            currentFiles.set(filePath, { ...file, content: fileDiff.originalFullContent });
            if (filePath === state.activeFilePath) {
              currentCode = fileDiff.originalFullContent;
            }
          }
        }
      }

      // 建立新 ChangeSet（按檔案分組，計算 stats）
      const changeSet = groupProposalsByFile(messageId, proposals);
      updatedChangeSets.set(messageId, changeSet);

      if (changeSet.fileChanges.length === 0) {
        return {
          ...state,
          files: currentFiles,
          code: currentCode,
          changeSets: updatedChangeSets,
          pendingChangeSetId: messageId,
        };
      }

      // 計算每個檔案的 fileDiff（originalFullContent + proposedFullContent）
      // proposedFullContent 由 mergeProposals 批次套用同檔案所有 proposals 計算得出
      // key 統一使用 resolvedPath（與 state.files 的 key 一致），避免 AI 相對/絕對路徑不一致
      const fileDiffs = new Map<
        string,
        { originalFullContent: string; proposedFullContent: string }
      >();
      for (const fileChange of changeSet.fileChanges) {
        const resolvedPath = resolveFilePath(currentFiles, fileChange.filePath);
        const file = resolvedPath ? currentFiles.get(resolvedPath) : undefined;
        const originalFullContent = file?.content ?? "";
        const { result: proposedFullContent } = mergeProposals(
          originalFullContent,
          fileChange.proposals
        );
        const key = resolvedPath ?? fileChange.filePath;
        fileDiffs.set(key, { originalFullContent, proposedFullContent });
      }

      // 更新 state.files 為 proposed 內容（讓編輯器顯示 proposed，diff 對比 original）
      for (const [filePath, fileDiff] of fileDiffs) {
        const file = currentFiles.get(filePath);
        if (file) {
          currentFiles.set(filePath, { ...file, content: fileDiff.proposedFullContent });
        }
      }

      // 進入 diff 模式，展示第一個異動檔案
      // 優先使用 fileDiffs 的 key（已 resolved），而非 fileChanges 的原始 filePath
      const firstFilePath = fileDiffs.keys().next().value ?? changeSet.fileChanges[0].filePath;
      const firstFileDiff = fileDiffs.get(firstFilePath)!;
      const openFiles = state.openFiles.includes(firstFilePath)
        ? state.openFiles
        : [...state.openFiles, firstFilePath];

      return {
        ...state,
        files: currentFiles,
        code: firstFileDiff.proposedFullContent,
        openFiles,
        activeFilePath: firstFilePath,
        changeSets: updatedChangeSets,
        pendingChangeSetId: messageId,
        diffReview: { changeSetMessageId: messageId, fileDiffs },
      };
    }

    case "APPLY_CHANGESET": {
      // 標記 ChangeSet 為已套用，清除 diff 模式。
      // 實際的檔案寫入（apiPut + setFileContent）已由 ChangeSetCard 處理完畢。
      const { messageId } = action.payload;
      const newChangeSets = new Map(state.changeSets);
      const cs = newChangeSets.get(messageId);
      if (cs) newChangeSets.set(messageId, { ...cs, status: "applied" });
      return {
        ...state,
        changeSets: newChangeSets,
        pendingChangeSetId: state.pendingChangeSetId === messageId ? null : state.pendingChangeSetId,
        diffReview:
          state.diffReview?.changeSetMessageId === messageId ? null : state.diffReview,
      };
    }

    case "REJECT_CHANGESET": {
      // 拒絕提案：還原檔案至 originalFullContent，清除 diff 模式。
      // 因為 REGISTER_CHANGESET 已把 state.files 更新為 proposed 內容，
      // 這裡需要從 fileDiffs 還原。後端無需操作（proposed 內容從未寫入後端）。
      const { messageId } = action.payload;
      const newChangeSets = new Map(state.changeSets);
      const cs = newChangeSets.get(messageId);
      if (cs) newChangeSets.set(messageId, { ...cs, status: "rejected" });

      let newFiles = new Map(state.files);
      let newCode = state.code;

      if (state.diffReview?.changeSetMessageId === messageId) {
        for (const [filePath, fileDiff] of state.diffReview.fileDiffs) {
          const file = newFiles.get(filePath);
          if (file) {
            newFiles.set(filePath, { ...file, content: fileDiff.originalFullContent });
            if (filePath === state.activeFilePath) {
              newCode = fileDiff.originalFullContent;
            }
          }
        }
      }

      return {
        ...state,
        changeSets: newChangeSets,
        pendingChangeSetId:
          state.pendingChangeSetId === messageId ? null : state.pendingChangeSetId,
        diffReview:
          state.diffReview?.changeSetMessageId === messageId ? null : state.diffReview,
        files: newFiles,
        code: newCode,
      };
    }

    case "CHECKPOINT_RESULT": {
      const { result } = action.payload;
      // 同步更新 checkpoints[] 中對應 entry 的 status，讓 stepper 可以反映最新狀態
      const updatedCheckpoints = state.checkpoints.map(c =>
        c.checkpointId === result.checkpointId ? { ...c, status: result.status } : c
      );
      return {
        ...state,
        isSubmitting: false,
        isRunning: false,
        checkpoint: result,
        aiEnabled: result.aiEnabled ?? true,
        lastExecution: action.payload.execution,
        checkpoints: updatedCheckpoints,
      };
    }
    default:
      return state;
  }
}

interface InterviewContextValue {
  state: InterviewState;
  setCode: (code: string) => void;
  setFileContent: (filePath: string, content: string) => void;
  setActiveFile: (filePath: string) => void;
  openFile: (filePath: string) => void;
  closeFile: (filePath: string) => void;
  setCheckpoint: (checkpoint: CheckpointResultResponse) => void;
  // 使用者點擊 stepper 跳選 checkpoint 時呼叫（切換 active checkpoint）
  switchCheckpoint: (checkpointId: string) => void;
  setSubmitting: (v: boolean) => void;
  setRunning: (v: boolean) => void;
  setExecution: (result: ExecutionResponse) => void;
  setStatus: (status: InterviewStatus) => void;
  applyCheckpointResult: (
    result: CheckpointResultResponse,
    execution: ExecutionResponse | null
  ) => void;
  loadFiles: (files: Map<string, CheckpointFileState>) => void;
  // ChangeSet 批次審查 API（新設計，取代舊的 enterDiffReview/exitDiffReview）
  /** 收到 AI proposals → 建立 ChangeSet + 自動進入 diff 模式 */
  registerChangeSet: (messageId: string, proposals: EditProposalData[]) => void;
  /** 使用者同意 → 標記 applied，清除 diff 模式（實際 apiPut 由 ChangeSetCard 處理） */
  applyChangeSet: (messageId: string) => void;
  /** 使用者拒絕 → 還原檔案內容，清除 diff 模式 */
  rejectChangeSet: (messageId: string) => void;
  // 由 AiChatPanel 透過 registerSendAiMessage 注入後，讓其他元件可送出 AI 訊息。
  // 若 AiChatPanel 尚未掛載則呼叫為 no-op（ref 尚未被設定）。
  sendAiMessage: (text: string) => void;
  registerSendAiMessage: (fn: (text: string) => void) => void;
}

const InterviewContext = createContext<InterviewContextValue | null>(null);

interface InterviewProviderProps {
  interviewId: string;
  initialStatus: InterviewStatus;
  initialCheckpoint: CheckpointResultResponse | null;
  // 保留完整 CheckpointResultResponse[]，包含 status 供 stepper 資料驅動判斷
  initialCheckpoints: CheckpointResultResponse[];
  initialFiles: Map<string, CheckpointFileState>;
  children: ReactNode;
}

export function InterviewProvider({
  interviewId,
  initialStatus,
  initialCheckpoint,
  initialCheckpoints,
  initialFiles,
  children,
}: InterviewProviderProps) {
  const readmePath = findReadme(initialFiles);
  const initialOpenFiles = readmePath ? [readmePath] : [];
  const initialActiveFile = readmePath;

  const [state, dispatch] = useReducer(reducer, {
    interviewId,
    status: initialStatus,
    checkpoint: initialCheckpoint,
    checkpoints: initialCheckpoints,
    aiEnabled: initialCheckpoint?.aiEnabled ?? true,
    files: initialFiles,
    openFiles: initialOpenFiles,
    activeFilePath: initialActiveFile,
    code: initialActiveFile ? (initialFiles.get(initialActiveFile)?.content ?? "") : "",
    isSubmitting: false,
    isRunning: false,
    lastExecution: null,
    diffReview: null,
    changeSets: new Map<string, ChangeSet>(),
    pendingChangeSetId: null,
  });

  const setCode = useCallback((code: string) => {
    dispatch({ type: "SET_CODE", payload: code });
  }, []);

  const setFileContent = useCallback((filePath: string, content: string) => {
    dispatch({ type: "SET_FILE_CONTENT", payload: { filePath, content } });
  }, []);

  const setActiveFile = useCallback((filePath: string) => {
    dispatch({ type: "SET_ACTIVE_FILE", payload: filePath });
  }, []);

  const openFile = useCallback((filePath: string) => {
    dispatch({ type: "OPEN_FILE", payload: filePath });
  }, []);

  const closeFile = useCallback((filePath: string) => {
    dispatch({ type: "CLOSE_FILE", payload: filePath });
  }, []);

  const setCheckpoint = useCallback((checkpoint: CheckpointResultResponse) => {
    dispatch({ type: "SET_CHECKPOINT", payload: checkpoint });
  }, []);

  const switchCheckpoint = useCallback((checkpointId: string) => {
    dispatch({ type: "SWITCH_CHECKPOINT", payload: checkpointId });
  }, []);

  const setSubmitting = useCallback((v: boolean) => {
    dispatch({ type: "SET_SUBMITTING", payload: v });
  }, []);

  const setRunning = useCallback((v: boolean) => {
    dispatch({ type: "SET_RUNNING", payload: v });
  }, []);

  const setExecution = useCallback((result: ExecutionResponse) => {
    dispatch({ type: "SET_EXECUTION", payload: result });
  }, []);

  const setStatus = useCallback((status: InterviewStatus) => {
    dispatch({ type: "SET_STATUS", payload: status });
  }, []);

  const applyCheckpointResult = useCallback(
    (result: CheckpointResultResponse, execution: ExecutionResponse | null) => {
      dispatch({ type: "CHECKPOINT_RESULT", payload: { result, execution } });
    },
    []
  );

  const loadFiles = useCallback((files: Map<string, CheckpointFileState>) => {
    dispatch({ type: "LOAD_FILES", payload: files });
  }, []);

  const registerChangeSet = useCallback(
    (messageId: string, proposals: EditProposalData[]) => {
      dispatch({ type: "REGISTER_CHANGESET", payload: { messageId, proposals } });
    },
    []
  );

  const applyChangeSet = useCallback((messageId: string) => {
    dispatch({ type: "APPLY_CHANGESET", payload: { messageId } });
  }, []);

  const rejectChangeSet = useCallback((messageId: string) => {
    dispatch({ type: "REJECT_CHANGESET", payload: { messageId } });
  }, []);

  // sendAiMessage 由 AiChatPanel 透過 registerSendAiMessage 注入。
  // 使用 ref + stable wrapper，讓外部元件呼叫時永遠取到最新的 append 函式，
  // 且不會因 ref 更新而觸發不必要的 context re-render。
  const sendAiMessageRef = useRef<((text: string) => void) | null>(null);
  const registerSendAiMessage = useCallback((fn: (text: string) => void) => {
    sendAiMessageRef.current = fn;
  }, []);
  const sendAiMessage = useCallback((text: string) => {
    sendAiMessageRef.current?.(text);
  }, []);

  return (
    <InterviewContext.Provider
      value={{
        state,
        setCode,
        setFileContent,
        setActiveFile,
        openFile,
        closeFile,
        setCheckpoint,
        switchCheckpoint,
        setSubmitting,
        setRunning,
        setExecution,
        setStatus,
        applyCheckpointResult,
        loadFiles,
        registerChangeSet,
        applyChangeSet,
        rejectChangeSet,
        sendAiMessage,
        registerSendAiMessage,
      }}
    >
      {children}
    </InterviewContext.Provider>
  );
}

export function useInterview() {
  const ctx = useContext(InterviewContext);
  if (!ctx) throw new Error("useInterview must be used within InterviewProvider");
  return ctx;
}
