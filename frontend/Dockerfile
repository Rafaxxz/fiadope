# ---- build ----
FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npx ng build --configuration production

# ---- runtime: nginx sirve la SPA y hace de proxy a la API (mismo dominio, sin CORS) ----
FROM nginx:1.27-alpine
ENV BACKEND_URL=http://api:8080
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist/fiadope-web/browser /usr/share/nginx/html
EXPOSE 80

