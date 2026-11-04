FROM node:24-alpine AS compilacion

WORKDIR /app
COPY package.json pnpm-lock.yaml ./
RUN corepack enable && pnpm install --frozen-lockfile
COPY index.html vite.config.js ./
COPY src src
RUN pnpm build

FROM caddy:2-alpine
COPY --from=compilacion /app/dist /usr/share/caddy
COPY Caddyfile /etc/caddy/Caddyfile
EXPOSE 80 443
