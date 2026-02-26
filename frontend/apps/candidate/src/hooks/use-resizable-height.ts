"use client";

import { useRef, useState } from "react";

interface UseResizableHeightOptions {
  initialHeight: number;
  minHeight?: number;
  maxHeight?: number;
}

export function useResizableHeight({
  initialHeight,
  minHeight = 100,
  maxHeight = 500,
}: UseResizableHeightOptions) {
  const [height, setHeight] = useState(initialHeight);
  const heightRef = useRef(initialHeight);
  heightRef.current = height;

  function onMouseDown(e: React.MouseEvent) {
    e.preventDefault();
    const startY = e.clientY;
    const startHeight = heightRef.current;

    document.body.style.cursor = "row-resize";
    document.body.style.userSelect = "none";

    function onMouseMove(e: MouseEvent) {
      // Terminal 在底部，把手在 Terminal 上方
      // 往上拖（delta < 0）= 高度增加
      const delta = e.clientY - startY;
      const next = Math.max(minHeight, Math.min(maxHeight, startHeight - delta));
      setHeight(next);
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

  return { height, onMouseDown };
}
