# Stage 1: Build static export
FROM node:24-alpine AS builder
WORKDIR /app

ARG NEXT_PUBLIC_API_BASE
ARG NEXT_PUBLIC_CANDIDATE_URL
ENV NEXT_PUBLIC_API_BASE=${NEXT_PUBLIC_API_BASE}
ENV NEXT_PUBLIC_CANDIDATE_URL=${NEXT_PUBLIC_CANDIDATE_URL}

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
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
