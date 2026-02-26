"use client";

import {
  createContext,
  useContext,
  useReducer,
  useCallback,
  type ReactNode,
} from "react";
import type {
  CheckpointResultResponse,
  CheckpointFileState,
  InterviewStatus,
  ExecutionResponse,
} from "@interview/shared/types";

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
  | {
      type: "CHECKPOINT_RESULT";
      payload: { result: CheckpointResultResponse; execution: ExecutionResponse | null };
    };

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
