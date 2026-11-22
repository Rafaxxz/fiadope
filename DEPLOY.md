# Despliegue de FiadoPe

FiadoPe tiene tres piezas: **PostgreSQL**, la **API Spring Boot** (`backend/`) y la **web Angular** (`frontend/`).
La web llama a la API en el mismo dominio (`/api`), así que en producción no hace falta CORS: nginx o Vercel reenvían
`/api` y `/uploads` al backend.

Hay dos caminos. Elige uno:

| Opción | Para qué | Costo |
|--------|----------|-------|
| **A. Un servidor con Docker Compose** | Una VPS (DigitalOcean, Lightsail, Oracle Free, Azure) o tu PC para la demo | Desde S/ 0 (Oracle Free Tier) |
| **B. Servicios administrados** | Neon (BD) + Render (API) + Vercel (web), sin administrar servidores | Plan gratuito (la API “duerme” sin uso) |

---

## Antes de empezar (las dos opciones)

1. **Secreto JWT** (32+ caracteres aleatorios):
   ```bash
   openssl rand -base64 48
   ```
2. **Correo para el MFA.** Con Gmail: activa la verificación en dos pasos y crea una *contraseña de aplicación*
   (Cuenta de Google → Seguridad → Contraseñas de aplicaciones). Usa `smtp.gmail.com`, puerto `587`.
   Sin SMTP el sistema funciona, pero el código solo se escribe en el log de la API.
3. **`MFA_DEV_ECHO`**: déjalo en `false` en producción. Con `true` la API devuelve el código MFA en la respuesta
   y la pantalla P02 lo muestra (útil solo para una demo en clase).

---

## Opción A · Docker Compose (todo en un servidor)

Requisitos: Docker 24+ con Compose.

```bash
git clone https://github.com/Rafaxxz/fiadope.git
cd fiadope
git checkout frontend          # rama con backend + frontend
cp .env.example .env           # completa DB_PASSWORD, JWT_SECRET y el SMTP
docker compose up -d --build
```

- Web: `http://IP-DEL-SERVIDOR` (puerto 80; cámbialo con `WEB_PORT`).
- La API no se publica: solo es accesible a través de la web (`/api`).
- Datos persistentes en los volúmenes `pgdata` (base) y `uploads` (fotos).
- Logs: `docker compose logs -f api`.
- Actualizar: `git pull && docker compose up -d --build`.

### HTTPS

Pon un proxy con certificado delante del puerto 80. Lo más simple es Caddy:

```bash
# /etc/caddy/Caddyfile
fiadope.tu-dominio.pe {
    reverse_proxy localhost:80
}
```

Caddy obtiene y renueva el certificado de Let's Encrypt (TLS 1.3) solo. Luego pon
`CORS_ORIGINS=https://fiadope.tu-dominio.pe` en `.env` y reinicia (`docker compose up -d`).

### Respaldo diario de la base

```bash
# crontab -e   (todos los días a las 3:00)
0 3 * * * docker compose -f /ruta/fiadope/docker-compose.yml exec -T db pg_dump -U fiadope -Fc fiadope > /respaldos/fiadope-$(date +\%F).dump
```

Restaurar: `docker compose exec -T db pg_restore -U fiadope -d fiadope --clean < archivo.dump`.

---

## Opción B · Neon + Render + Vercel

### 1. Base de datos en Neon

1. Crea un proyecto en <https://neon.tech> (región São Paulo, la más cercana a Lima).
2. Copia el *connection string* y conviértelo a JDBC:
   ```
   DB_URL=jdbc:postgresql://<host>.neon.tech/neondb?sslmode=require
   DB_USER=<usuario>
   DB_PASSWORD=<clave>
   ```
   Flyway crea las tablas en el primer arranque.

### 2. API en Render

1. <https://render.com> → **New → Web Service** → conecta el repo `Rafaxxz/fiadope`.
2. **Root Directory:** `backend` · **Runtime:** Docker (usa `backend/Dockerfile`).
3. **Health Check Path:** `/actuator/health`.
4. Variables de entorno:

   | Variable | Valor |
   |----------|-------|
   | `DB_URL`, `DB_USER`, `DB_PASSWORD` | Los de Neon |
   | `JWT_SECRET` | El generado con openssl |
   | `MFA_DEV_ECHO` | `false` |
   | `SEED_ENABLED` | `true` para cargar la demo la primera vez, luego `false` |
   | `CORS_ORIGINS` | `https://tu-app.vercel.app` |
   | `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` | Tu SMTP |

5. Anota la URL pública, por ejemplo `https://fiadope-api.onrender.com`.

> Las fotos que suban las tiendas se guardan en el disco del contenedor. En el plan gratuito ese disco se borra en
> cada despliegue; para conservarlas agrega un *Persistent Disk* montado en `/app/uploads`.

### 3. Web en Vercel

1. Edita `frontend/vercel.json` y reemplaza `https://TU-API.onrender.com` por la URL de Render (dos líneas). Haz commit.
2. <https://vercel.com> → **Add New → Project** → importa el repo, rama `frontend`.
3. **Root Directory:** `frontend`. Vercel lee el build y los rewrites de `vercel.json`.
4. Despliega. La web queda en `https://tu-app.vercel.app` y llama a la API como `/api/...`.

---

## Verificación después de desplegar

1. `GET https://<api>/actuator/health` responde `{"status":"UP"}`.
2. Entra a la web con `10458792011` / `Bodega#Lucho26` (si cargaste la demo): debe pedir captcha y luego el
   código de 6 dígitos que llega al correo de la tienda.
3. En **Registrar pago**, elige a Rosa Quispe y pon la fecha **29/09/2026**: el total debe ser **S/ 208.18**
   (207.57 + 0.37 de interés compensatorio + 0.24 de moratorio), igual que el juego de prueba 1 del informe.
4. Cambia las contraseñas de los usuarios de demostración o crea tiendas reales desde el administrador de plataforma
   (`70000000`) y pon `SEED_ENABLED=false`.

## Checklist de producción

- [ ] `JWT_SECRET` aleatorio de 32+ caracteres (nunca el valor por defecto).
- [ ] `MFA_DEV_ECHO=false`.
- [ ] SMTP configurado y probado.
- [ ] HTTPS activo (TLS 1.3) y `CORS_ORIGINS` con el dominio real.
- [ ] Respaldos diarios de la base (Neon los hace solo; en Docker, el cron de arriba).
- [ ] Contraseñas de demostración cambiadas o `SEED_ENABLED=false` en una base nueva.
- [ ] Topes del BCRP actualizados por el administrador de plataforma cada semestre (mayo y noviembre).
- [ ] Banco de datos de clientes inscrito ante la Autoridad Nacional de Protección de Datos Personales (Ley 29733, art. 34).

## Desarrollo local (sin Docker)

```bash
# Terminal 1 — API (PostgreSQL local con una base vacía llamada fiadope)
cd backend
DB_PASSWORD=tu_clave ./mvnw spring-boot:run

# Terminal 2 — Web
cd frontend
npm install
npx ng serve            # http://localhost:4200
```
