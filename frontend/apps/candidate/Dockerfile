# Stage 1: Build static export
FROM node:24-alpine AS builder
WORKDIR /app
COPY package.json ./
COPY packages/shared/ packages/shared/
COPY apps/candidate/ apps/candidate/
RUN npm install --workspace=apps/candidate
WORKDIR /app/apps/candidate
RUN npm run build:export

# Stage 2: Serve with Nginx
FROM nginx:1.28-alpine
COPY --from=builder /app/apps/candidate/out /usr/share/nginx/html
COPY apps/candidate/nginx.conf /etc/nginx/nginx.conf
COPY apps/candidate/docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh
EXPOSE 80
ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["nginx", "-g", "daemon off;"]
