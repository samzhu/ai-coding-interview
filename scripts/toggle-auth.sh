#!/usr/bin/env bash
# 設計說明：一鍵切換 dev 環境認證，同時修改後端 yaml 和前端 .env.local。
# 用法: ./scripts/toggle-auth.sh [on|off]  （無參數顯示目前狀態）
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

BACKEND_CONFIG="$ROOT_DIR/backend/config/application-dev.yaml"
FRONTEND_ENV="$ROOT_DIR/frontend/apps/admin/.env.local"

# 偵測目前狀態（讀後端 yaml）
current_state() {
  if grep -q 'aci-security-enabled: true' "$BACKEND_CONFIG" 2>/dev/null; then
    echo "on"
  else
    echo "off"
  fi
}

show_help() {
  cat <<EOF
用法: $(basename "$0") [on|off|-h]

  （無參數）  顯示目前認證狀態
  on          開啟認證（JWT 驗證 + OIDC 登入，需重啟服務）
  off         關閉認證（permitAll + DevAuthProvider，需重啟服務）
  -h, --help  顯示此說明

修改範圍：
  後端  backend/config/application-dev.yaml  → aci-security-enabled
  前端  frontend/apps/admin/.env.local       → NEXT_PUBLIC_OIDC_* 變數

範例：
  ./scripts/toggle-auth.sh          # 查看目前狀態
  ./scripts/toggle-auth.sh off      # 關閉認證
  ./scripts/toggle-auth.sh on       # 開啟認證
EOF
}

show_status() {
  local state
  state=$(current_state)
  if [ "$state" = "on" ]; then
    echo "認證狀態: ON（JWT 驗證 + OIDC 登入）"
  else
    echo "認證狀態: OFF（permitAll + DevAuthProvider）"
  fi
}

turn_off() {
  # 後端：true → false
  sed -i '' 's/aci-security-enabled: true/aci-security-enabled: false/' "$BACKEND_CONFIG"
  # 前端：註解 OIDC 變數（只處理未被註解的行）
  sed -i '' 's/^NEXT_PUBLIC_OIDC_/# NEXT_PUBLIC_OIDC_/' "$FRONTEND_ENV"
  echo "✓ 認證已關閉（需重啟後端和前端）"
}

turn_on() {
  # 後端：false → true
  sed -i '' 's/aci-security-enabled: false/aci-security-enabled: true/' "$BACKEND_CONFIG"
  # 前端：取消註解 OIDC 變數
  sed -i '' 's/^# NEXT_PUBLIC_OIDC_/NEXT_PUBLIC_OIDC_/' "$FRONTEND_ENV"
  echo "✓ 認證已開啟（需重啟後端和前端）"
}

case "${1:-}" in
  off)         turn_off ;;
  on)          turn_on ;;
  "")          show_status ;;
  -h|--help)   show_help ;;
  *)           echo "錯誤：未知參數 '$1'"; echo; show_help; exit 1 ;;
esac
