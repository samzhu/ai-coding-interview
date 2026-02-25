"use client";

import { useRef, useState } from "react";

interface UseResizablePanelOptions {
  initialWidth: number;
  minWidth?: number;
  maxWidth?: number;
  /** reversed=true: 拖曳把手在 panel 左側，往左拖寬度增加（右側 panel 用） */
  reversed?: boolean;
}

export function useResizablePanel({
  initialWidth,
  minWidth = 100,
  maxWidth = 500,
  reversed = false,
}: UseResizablePanelOptions) {
  const [width, setWidth] = useState(initialWidth);
  const widthRef = useRef(initialWidth);
  widthRef.current = width;

  function onMouseDown(e: React.MouseEvent) {
    e.preventDefault();
    const startX = e.clientX;
    const startWidth = widthRef.current;

    document.body.style.cursor = "col-resize";
    document.body.style.userSelect = "none";

    function onMouseMove(e: MouseEvent) {
      const delta = e.clientX - startX;
      const next = Math.max(minWidth, Math.min(maxWidth, startWidth + (reversed ? -delta : delta)));
      setWidth(next);
    }

    function onMouseUp() {
      document.body.style.cursor = "";
      document.body.style.userSelect = "";
      document.removeEventListener("mousemove", onMouseMove);
      document.removeEventListener("mouseup", onMouseUp);
    }

    document.addEventListener("mousemove", onMouseMove);
    document.addEventListener("mouseup", onMouseUp);
  }

  return { width, onMouseDown };
}
