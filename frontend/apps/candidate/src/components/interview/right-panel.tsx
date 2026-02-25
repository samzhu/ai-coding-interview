"use client";

import { useState } from "react";
import { CheckpointPanel } from "./checkpoint-panel";
import { AiChatPanel } from "./ai-chat-panel";
import { cn } from "@interview/shared/lib/utils";

interface RightPanelProps {
  interviewId: string;
  aiEnabled?: boolean;
  onClose: () => void;
  onRun: () => void;
  isRunning: boolean;
  onEndInterview: () => void;
}

type TabId = "question" | "ai";

const TABS: { id: TabId; label: string }[] = [
  { id: "question", label: "📋 題目" },
  { id: "ai", label: "🤖 AI 助理" },
];

export function RightPanel({ interviewId, aiEnabled, onClose, onRun, isRunning, onEndInterview }: RightPanelProps) {
  const [activeTab, setActiveTab] = useState<TabId>("ai");

  return (
    <div className="flex flex-col h-full bg-[#1e1e1e]">
      {/* Tab bar */}
      <div className="flex items-center bg-[#252526] border-b border-[#333] shrink-0">
        {TABS.map((tab) => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id)}
            className={cn(
              "flex items-center gap-1.5 px-4 py-2 text-xs font-medium transition-colors",
              activeTab === tab.id
                ? "text-[#cccccc] border-b-2 border-b-[#007acc]"
                : "text-[#858585] hover:text-[#cccccc]"
            )}
          >
            {tab.label}
          </button>
        ))}
        <div className="flex-1" />
        <button
          onClick={onClose}
          className="px-3 py-2 text-[#858585] hover:text-[#cccccc] transition-colors text-sm leading-none"
          title="關閉右側面板"
        >
          ✕
        </button>
      </div>

      {/* Tab content */}
      <div className="flex-1 overflow-hidden">
        {activeTab === "question" ? (
          <CheckpointPanel onRun={onRun} isRunning={isRunning} onEndInterview={onEndInterview} />
        ) : (
          <AiChatPanel interviewId={interviewId} aiEnabled={aiEnabled} />
        )}
      </div>
    </div>
  );
}
