"use client";
import { useEffect, useMemo, useState } from "react";

// 設計說明：瀏覽器無法掃描靜態資料夾，用常數陣列註冊可用動畫。
// 新增動畫時：放精靈圖到 public/sprites/<name>/sprite_sheet.png + sprite_sheet.json，
// 然後把 <name> 加到這裡。
const SPRITE_ANIMATIONS = ["cow"];
const FPS = 8;
const VIEW_HEIGHT = 60;

interface SpriteFrame {
  width: number;
  height: number;
  x: number;
  y: number;
}
interface SpriteSheet {
  sprites: SpriteFrame[];
  spriteSheetWidth: number;
  spriteSheetHeight: number;
}

// 像素風精靈圖動畫：每次 mount 隨機挑一個動畫，右→左穿越畫面。
// 兩層動畫合成：
//   1. JS setInterval — 逐幀切換 background-position（腳步動畫）
//   2. CSS animate-walk-across — left 100% → -80px（走過畫面）
// 縮放邏輯：以 VIEW_HEIGHT / sprite.height 為統一縮放比，overflow-hidden 裁掉相鄰幀。
export function ThinkingAnimation({ thinkingSeconds }: { thinkingSeconds: number }) {
  const animationName = useMemo(
    () => SPRITE_ANIMATIONS[Math.floor(Math.random() * SPRITE_ANIMATIONS.length)],
    []
  );
  const [spriteSheet, setSpriteSheet] = useState<SpriteSheet | null>(null);
  const [frame, setFrame] = useState(0);

  useEffect(() => {
    fetch(`/sprites/${animationName}/sprite_sheet.json`)
      .then((r) => r.json())
      .then(setSpriteSheet)
      .catch(() => {});
  }, [animationName]);

  useEffect(() => {
    if (!spriteSheet) return;
    const interval = setInterval(
      () => setFrame((f) => (f + 1) % spriteSheet.sprites.length),
      1000 / FPS
    );
    return () => clearInterval(interval);
  }, [spriteSheet]);

  if (!spriteSheet) {
    return (
      <div className="relative overflow-hidden h-[72px] mx-3 my-1" role="status" aria-live="polite">
        <span className="absolute bottom-1 left-0 text-[10px] text-[#585858]">
          AI 思考中...{thinkingSeconds > 0 ? ` (${thinkingSeconds}s)` : ""}
        </span>
      </div>
    );
  }

  // 設計說明：以 viewHeight / 原始幀高 為縮放比，保持等比例縮放。
  // viewWidth 使用完整的縮放後寬度，不裁切，確保角色頭尾完整顯示。
  // backgroundPosition 直接用 JSON 的 sprite.x 座標定位，不需手動計算偏移。
  const firstSprite = spriteSheet.sprites[0];
  const scale = VIEW_HEIGHT / firstSprite.height;
  const viewWidth = Math.round(firstSprite.width * scale);
  const scaledSheetWidth = spriteSheet.spriteSheetWidth * scale;
  const currentSprite = spriteSheet.sprites[frame];

  return (
    <div className="relative overflow-hidden h-[72px] mx-3 my-1" role="status" aria-live="polite">
      <div
        className="absolute top-0 select-none animate-walk-across overflow-hidden"
        style={{
          width: viewWidth,
          height: VIEW_HEIGHT,
          backgroundImage: `url(/sprites/${animationName}/sprite_sheet.png)`,
          backgroundSize: `${scaledSheetWidth}px auto`,
          backgroundPosition: `-${currentSprite.x * scale}px center`,
          backgroundRepeat: "no-repeat",
        }}
      />
      <span className="absolute bottom-1 left-0 text-[10px] text-[#585858]">
        AI 思考中...{thinkingSeconds > 0 ? ` (${thinkingSeconds}s)` : ""}
      </span>
    </div>
  );
}
