# Stage 1: Build static export
FROM node:24-alpine AS builder
WORKDIR /app

ARG NEXT_PUBLIC_API_BASE
ENV NEXT_PUBLIC_API_BASE=${NEXT_PUBLIC_API_BASE}

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
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
