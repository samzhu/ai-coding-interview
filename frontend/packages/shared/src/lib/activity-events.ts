/**
 * 設計說明：Activity event client — 用 fetch 而非 axios，符合專案無 axios 慣例。
 * 失敗不重試也不阻塞 — L1 設計就接受斷線期間遺失（候選人此時也無法操作）。
 * 用 keepalive: true 讓頁面關閉時的 in-flight request 仍能送出。
 *
 * 使用 getApiBase 模式與 api-client.ts 保持一致，dev 模式下走相對路徑 /api/v1。
 */

export type ActivityEventType =
  | "file.opened"
  | "file.closed"
  | "terminal.test.run"
  | "code.manual_edit";

export type ActivityEventPayload = Record<string, unknown>;

export type ActivityEvent = {
  eventType: ActivityEventType;
  payload: ActivityEventPayload;
};

function getActivityApiBase(): string {
  return (process.env.NEXT_PUBLIC_API_BASE ?? "") + "/api/v1";
}

export async function postActivityEvent(
  interviewId: string,
  event: ActivityEvent,
): Promise<void> {
  const url = `${getActivityApiBase()}/interviews/${interviewId}/activity`;
  try {
    await fetch(url, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(event),
      keepalive: true,
    });
  } catch (e) {
    // 設計說明：activity event 失敗不應該影響 UX。記 console 即可。
    console.warn("Failed to post activity event", event.eventType, e);
  }
}

/**
 * Debounce wrapper for high-frequency events (e.g. code.manual_edit triggered
 * by every keystroke). Returns a function that batches the latest event into
 * one POST after `delayMs` of inactivity.
 *
 * 設計說明：每次 call 都覆蓋 pending event，因為 pilot score 只在乎「最終狀態」
 * 而非中間 keystroke 序列。flushNow() 給頁面卸載前手動觸發用。
 */
export function createDebouncedActivityPoster(
  interviewId: string,
  delayMs = 2000,
): { post: (event: ActivityEvent) => void; flushNow: () => void } {
  let timer: ReturnType<typeof setTimeout> | null = null;
  let pending: ActivityEvent | null = null;

  function flushNow() {
    if (timer) {
      clearTimeout(timer);
      timer = null;
    }
    if (pending) {
      postActivityEvent(interviewId, pending);
      pending = null;
    }
  }

  function post(event: ActivityEvent) {
    pending = event;
    if (timer) clearTimeout(timer);
    timer = setTimeout(flushNow, delayMs);
  }

  return { post, flushNow };
}
