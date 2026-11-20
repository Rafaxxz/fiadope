# Despliegue de FiadoPe

> Esta guía cubre el backend. La parte del frontend se completa en la rama `frontend`.

## 1. Base de datos (PostgreSQL 16+)

Cualquier PostgreSQL administrado sirve (Neon, Supabase, Render PostgreSQL, Railway, RDS). Crea una base vacía llamada
`fiadope`; Flyway crea las tablas al primer arranque.

```
DB_URL=jdbc:postgresql://<host>:5432/fiadope?sslmode=require
DB_USER=<usuario>
DB_PASSWORD=<clave>
```

## 2. Backend (Docker)

```bash
cd backend
docker build -t fiadope-api .
docker run -p 8080:8080 \
  -e DB_URL=... -e DB_USER=... -e DB_PASSWORD=... \
  -e JWT_SECRET="$(openssl rand -base64 48)" \
  -e MFA_DEV_ECHO=false \
  -e CORS_ORIGINS=https://tu-frontend.example.com \
  -e MAIL_HOST=smtp.gmail.com -e MAIL_USERNAME=... -e MAIL_PASSWORD=... -e MAIL_FROM=... \
  fiadope-api
```

En Render: *New → Web Service → Docker*, carpeta raíz `backend`, health check `/actuator/health`, y las variables de arriba.

## 3. Checklist de producción

- [ ] `JWT_SECRET` aleatorio de 32+ caracteres.
- [ ] `MFA_DEV_ECHO=false` (si no, la API devuelve el código MFA).
- [ ] SMTP configurado (sin SMTP el código solo se escribe en el log).
- [ ] `SEED_ENABLED=false` si no quieres los datos de demostración.
- [ ] HTTPS delante de la API (TLS 1.3 en el balanceador o el proveedor).
- [ ] Respaldos diarios de la base activados en el proveedor.
