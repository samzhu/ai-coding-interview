"use client";

/**
 * xterm.js 互動式終端核心元件（SSR-disabled）。
 *
 * 設計說明：
 * - xterm.js 直接操作 DOM，無法在 SSR 環境執行，必須透過 dynamic import + ssr:false 載入。
 * - WebSocket 直接連後端（不經過 Next.js proxy），因為 Next.js rewrite 不支援 WebSocket 升級。
 * - 通訊協定使用單字元前綴（i/r/o/e），避免 JSON 解析延遲，對終端機互動至關重要。
 * - FitAddon 讓終端自動適配容器寬高；ResizeObserver 偵測容器大小變化並通知後端 resize。
 * - isActive prop：tab 切換時，hidden 的 tab 寬高為 0，需等 visible 後再 fit 一次。
 */

import { useEffect, useRef } from "react";
import { Terminal } from "@xterm/xterm";
import { FitAddon } from "@xterm/addon-fit";
import { WebLinksAddon } from "@xterm/addon-web-links";
import { getWsUrl } from "@interview/shared/lib/api-client";

interface XtermTerminalInnerProps {
  interviewId: string;
  isActive: boolean;
}

// VS Code 深色主題色調，與工作區整體風格一致
const VS_CODE_THEME = {
  background: "#1e1e1e",
  foreground: "#d4d4d4",
  cursor: "#aeafad",
  cursorAccent: "#1e1e1e",
  selectionBackground: "#264f78",
  black: "#1e1e1e",
  red: "#f44747",
  green: "#608b4e",
  yellow: "#dcdcaa",
  blue: "#569cd6",
  magenta: "#c678dd",
  cyan: "#4ec9b0",
  white: "#d4d4d4",
  brightBlack: "#808080",
  brightRed: "#f44747",
  brightGreen: "#4ec9b0",
  brightYellow: "#dcdcaa",
  brightBlue: "#569cd6",
  brightMagenta: "#c678dd",
  brightCyan: "#4ec9b0",
  brightWhite: "#d4d4d4",
};

export default function XtermTerminalInner({
  interviewId,
  isActive,
}: XtermTerminalInnerProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const terminalRef = useRef<Terminal | null>(null);
  const fitAddonRef = useRef<FitAddon | null>(null);
  const wsRef = useRef<WebSocket | null>(null);

  // 初始化 xterm.js 和 WebSocket 連線
  useEffect(() => {
    if (!containerRef.current) return;

    const terminal = new Terminal({
      theme: VS_CODE_THEME,
      fontSize: 13,
      fontFamily: "Menlo, Monaco, 'Courier New', monospace",
      cursorBlink: true,
      scrollback: 1000,
      // convertEol: true 讓 \n 自動換行（部分 bash 輸出只送 \n）
      convertEol: true,
    });

    const fitAddon = new FitAddon();
    const webLinksAddon = new WebLinksAddon();
    terminal.loadAddon(fitAddon);
    terminal.loadAddon(webLinksAddon);
    terminal.open(containerRef.current);

    terminalRef.current = terminal;
    fitAddonRef.current = fitAddon;

    // 使用 requestAnimationFrame 確保 DOM 已完成佈局後再 fit
    requestAnimationFrame(() => fitAddon.fit());

    // 連線到後端 WebSocket terminal endpoint
    const wsUrl = getWsUrl(`/interviews/${interviewId}/terminal`);
    const ws = new WebSocket(wsUrl);
    wsRef.current = ws;

    ws.onopen = () => {
      // 連線建立後立即送出終端大小，讓後端 resize exec
      ws.send(`r${terminal.cols},${terminal.rows}`);
    };

    ws.onmessage = (e) => {
      const data = e.data as string;
      if (data.startsWith("o")) {
        // "o" 前綴：終端輸出
        terminal.write(data.slice(1));
      } else if (data.startsWith("e")) {
        // "e" 前綴：錯誤訊息，以紅色顯示
        terminal.write(`\r\n\x1b[31m[Error] ${data.slice(1)}\x1b[0m\r\n`);
      }
    };

    ws.onerror = () => {
      terminal.write("\r\n\x1b[31m[WebSocket error]\x1b[0m\r\n");
    };

    ws.onclose = (e) => {
      terminal.write(`\r\n\x1b[33m[Connection closed (${e.code})]\x1b[0m\r\n`);
    };

    // 使用者輸入 → 送出 "i" 前綴訊息
    terminal.onData((data) => {
      if (ws.readyState === WebSocket.OPEN) {
        ws.send("i" + data);
      }
    });

    // ResizeObserver 偵測容器尺寸變化，更新 xterm 和後端
    const resizeObserver = new ResizeObserver(() => {
      try {
        fitAddon.fit();
        if (ws.readyState === WebSocket.OPEN) {
          ws.send(`r${terminal.cols},${terminal.rows}`);
        }
      } catch {
        // fit() 在容器為 hidden 時可能拋出，忽略即可
      }
    });
    resizeObserver.observe(containerRef.current);

    return () => {
      resizeObserver.disconnect();
      ws.close();
      terminal.dispose();
    };
  }, [interviewId]);

  // tab 切換為 active 時重新 fit（hidden 狀態下 FitAddon 無法正確計算尺寸）
  useEffect(() => {
    if (isActive && fitAddonRef.current && terminalRef.current) {
      requestAnimationFrame(() => {
        try {
          fitAddonRef.current!.fit();
          const ws = wsRef.current;
          const t = terminalRef.current!;
          if (ws && ws.readyState === WebSocket.OPEN) {
            ws.send(`r${t.cols},${t.rows}`);
          }
        } catch {
          // 忽略
        }
      });
    }
  }, [isActive]);

  return <div ref={containerRef} className="h-full w-full overflow-hidden" />;
}
