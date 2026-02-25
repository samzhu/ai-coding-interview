"use client";

import { useState, useCallback } from "react";

export interface WorkspaceLayout {
  fileExplorer: boolean; // true = 展開
  rightPanel: boolean; // true = 展開
  terminal: boolean; // true = 展開
}

const DEFAULT_LAYOUT: WorkspaceLayout = {
  fileExplorer: true,
  rightPanel: true,
  terminal: false,
};

export function useWorkspaceLayout() {
  const [layout, setLayout] = useState<WorkspaceLayout>(DEFAULT_LAYOUT);

  const toggleFileExplorer = useCallback(() => {
    setLayout((l) => ({ ...l, fileExplorer: !l.fileExplorer }));
  }, []);

  const toggleRightPanel = useCallback(() => {
    setLayout((l) => ({ ...l, rightPanel: !l.rightPanel }));
  }, []);

  const toggleTerminal = useCallback(() => {
    setLayout((l) => ({ ...l, terminal: !l.terminal }));
  }, []);

  const resetLayout = useCallback(() => {
    setLayout(DEFAULT_LAYOUT);
  }, []);

  const autoExpandTerminal = useCallback(() => {
    setLayout((l) => ({ ...l, terminal: true }));
  }, []);

  return {
    layout,
    toggleFileExplorer,
    toggleRightPanel,
    toggleTerminal,
    resetLayout,
    autoExpandTerminal,
  };
}
