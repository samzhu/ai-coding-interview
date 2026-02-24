# Stage 1: Build static export
FROM node:24-alpine AS builder
WORKDIR /app
COPY package.json ./
COPY packages/shared/ packages/shared/
COPY apps/admin/ apps/admin/
RUN npm install --workspace=apps/admin
WORKDIR /app/apps/admin
RUN npm run build:export

# Stage 2: Serve with Nginx
FROM nginx:1.28-alpine
COPY --from=builder /app/apps/admin/out /usr/share/nginx/html
COPY apps/admin/nginx.conf /etc/nginx/nginx.conf
COPY apps/admin/docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh
EXPOSE 80
ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["nginx", "-g", "daemon off;"]
