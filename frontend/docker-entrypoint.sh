#!/bin/sh
# 替換靜態 JS 中的 placeholder 為實際 API URL
find /usr/share/nginx/html -name '*.js' -exec \
  sed -i "s|__API_BASE_PLACEHOLDER__|${API_BASE_URL:-}|g" {} +
exec "$@"
