# ───────── Etapa 1: compilar Angular con Node ─────────
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
# --configuration production usa environment.prod.ts (rutas relativas: /api, /login...)
RUN npx ng build --configuration production

# ───────── Etapa 2: nginx sirve los archivos y reenvía lo demás al backend ─────────
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist/sgad-frontend/ /usr/share/nginx/html/
# Valores para docker compose; en Railway se sobrescriben con variables (ver README_RAILWAY.md)
ENV PORT=80 \
    BACKEND_URL=http://backend:8080 \
    NGINX_RESOLVER=127.0.0.11
EXPOSE 80
