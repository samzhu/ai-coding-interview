"use client";

import { useEffect, useState } from "react";

// Sprite sheet 乳牛走路動畫參數：原圖 1536×1024, 7 幀水平排列
// CELL_WIDTH > VIEW_WIDTH 是關鍵：讓容器寬度小於 sprite cell，
// 配合 overflow-hidden 裁掉兩側邊緣，確保相鄰幀不會漏出可視範圍。
// CENTER_OFFSET 將 cell 置中於容器，避免只看到牛的左半邊。
const FRAME_COUNT = 7;
const CELL_WIDTH = 110;                          // 每個 sprite cell 的縮放寬度（刻意大於容器）
const VIEW_WIDTH = 80;                           // 容器可視寬度（裁掉兩側各 15px）
const VIEW_HEIGHT = 60;                          // 容器可視高度
const SPRITE_WIDTH = CELL_WIDTH * FRAME_COUNT;   // 770px（background-size 寬）
const CENTER_OFFSET = (CELL_WIDTH - VIEW_WIDTH) / 2; // 15px — 水平居中偏移
const FPS = 8;                                   // 走路幀率

interface ThinkingAnimationProps {
  thinkingSeconds: number;
}

// 像素風乳牛 sprite sheet 動畫：右→左穿越畫面。
// 兩層動畫合成：
//   1. JS setInterval — 逐幀切換 background-position-x（腳步動畫）
//   2. CSS animate-walk-across — left 100% → -80px（走過畫面）
// CELL_WIDTH(110) > VIEW_WIDTH(80) 確保只顯示單幀，overflow-hidden 裁掉鄰幀。
export function ThinkingAnimation({ thinkingSeconds }: ThinkingAnimationProps) {
  const [frame, setFrame] = useState(0);

  // 逐幀動畫：每 125ms (8fps) 切換下一幀
  useEffect(() => {
    const interval = setInterval(
      () => setFrame((f) => (f + 1) % FRAME_COUNT),
      1000 / FPS
    );
    return () => clearInterval(interval);
  }, []);

  return (
    <div className="relative overflow-hidden h-[72px] mx-3 my-1" role="status" aria-live="polite">
      <div
        className="absolute top-0 select-none animate-walk-across overflow-hidden"
        style={{
          width: VIEW_WIDTH,
          height: VIEW_HEIGHT,
          backgroundImage: "url(/sprites/cow-walk.png)",
          backgroundSize: `${SPRITE_WIDTH}px auto`,
          backgroundPosition: `-${frame * CELL_WIDTH + CENTER_OFFSET}px center`,
          backgroundRepeat: "no-repeat",
        }}
      />
      <span className="absolute bottom-1 left-0 text-[10px] text-[#585858]">
        AI 思考中...{thinkingSeconds > 0 ? ` (${thinkingSeconds}s)` : ""}
      </span>
    </div>
  );
}
