# FiadoPe · Deja el cuaderno, cobra con orden

Aplicación web multi-tienda que reemplaza el cuaderno de fiados de las bodegas, panaderías y pollerías del barrio por una
**cuenta corriente digital**. Calcula intereses, cuotas (método francés con gracia total) y mora de forma exacta y
transparente para el vecino.

Proyecto del curso **Finanzas e Ingeniería Económica (1ASI0642) – UPC**, Grupo 01.

| Capa | Tecnología | Carpeta | Rama |
|------|------------|---------|------|
| API REST + motor financiero | Java 17, Spring Boot 3.5, Spring Security (JWT + MFA), JPA, Flyway | `backend/` | `main` |
| Base de datos | PostgreSQL 16+ | `backend/src/main/resources/db/migration` | `main` |
| Frontend | Angular 20 + Tailwind (mismo diseño y colores de los mockups P01-P12) | `frontend/` | `frontend` |

El despliegue está en [`DEPLOY.md`](DEPLOY.md) (Docker Compose o Neon + Render + Vercel).

Para levantar todo con Docker: `cp .env.example .env`, completa los valores y `docker compose up -d --build`.

## Frontend

```bash
cd frontend
npm install
npx ng serve      # http://localhost:4200, usa la API de http://localhost:8080
```

Las 12 pantallas, sus rutas y roles están en [`frontend/README.md`](frontend/README.md).

---

## Backend

### Ejecutar en local

Requisitos: JDK 17+ y PostgreSQL. Maven no hace falta (se usa el wrapper `mvnw`).

```bash
createdb -U postgres fiadope            # una sola vez
cd backend
DB_PASSWORD=tu_clave ./mvnw spring-boot:run    # Windows: set DB_PASSWORD=tu_clave && mvnw spring-boot:run
```

- API: `http://localhost:8080`
- Swagger: `http://localhost:8080/swagger-ui.html`
- Salud: `http://localhost:8080/actuator/health`

Flyway crea las 11 tablas del informe (más `parametro` con los topes del BCRP) y, si la base está vacía, se cargan
los **juegos de prueba 1 y 2** usando los mismos servicios de la API.

### Variables de entorno

| Variable | Por defecto | Uso |
|----------|-------------|-----|
| `DB_URL` | `jdbc:postgresql://localhost:5432/fiadope` | Conexión JDBC |
| `DB_USER` / `DB_PASSWORD` | `postgres` / `postgres` | Credenciales de la base |
| `JWT_SECRET` | valor de desarrollo | **Obligatorio en producción** (32+ caracteres) |
| `MFA_DEV_ECHO` | `true` | Devuelve el código MFA en la respuesta del login (solo desarrollo). Poner `false` en producción |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` | vacío | SMTP para el código MFA y los estados de cuenta. Sin SMTP los correos se escriben en el log |
| `CORS_ORIGINS` | `http://localhost:4200` | Orígenes del frontend (separados por coma) |
| `UPLOADS_DIR` | `./uploads` | Carpeta de las fotos de productos |
| `SEED_ENABLED` | `true` | Carga los datos de demostración si la base está vacía |
| `PORT` | `8080` | Puerto HTTP |

### Usuarios de demostración

| Rol | Usuario | Contraseña |
|-----|---------|------------|
| Administrador de plataforma | `70000000` | `Admin#Fiado26` |
| Administrador de negocio · Bodega Don Lucho | `10458792011` | `Bodega#Lucho26` |
| Administrador de negocio · Panadería La Espiga | `20601234567` | `Espiga#Pan26` |
| Cliente · Rosa Quispe Mamani (juego 1) | `47891230` | `Rosa#Fiado26` |
| Cliente · Jorge Huamán Torres (juego 2) | `41234567` | `Jorge#Fiado26` |
| Tienda dada de baja (para probar CP14) | `10712345678` | `Brasero#Pollo26` |

El login pide captcha y luego el código de 6 dígitos que llega al correo. En desarrollo (`MFA_DEV_ECHO=true`) el código
también viene en la respuesta y se escribe en el log del servidor.

### Arquitectura

```
backend/src/main/java/pe/fiadope/api
├── finanzas/      MotorFinanciero: fórmulas F1 a F12 (puro, sin dependencias)
├── domain/        Entidades JPA (tienda, usuario, cliente, producto, compra, cuota, estado_cuenta, ...)
├── repository/    Spring Data JPA (toda consulta filtra por id_tienda)
├── service/       Reglas de negocio por historia de usuario (HU01 a HU12)
├── controller/    API REST
├── security/      JWT, sesiones con cierre por inactividad, captcha, filtro de seguridad
├── dto/           Contratos de entrada/salida con validaciones
└── seed/          Datos de los juegos de prueba
```

### Seguridad implementada

- Contraseñas con **bcrypt** (costo 12) y política de 8+ caracteres con mayúscula, número y símbolo.
- **Captcha** de 4 dígitos de un solo uso y **MFA** por correo: código de 6 dígitos, vence en 5 minutos, 3 intentos, reenviar invalida el anterior.
- **Bloqueo** de la cuenta por 15 minutos tras 5 contraseñas incorrectas (queda en la bitácora).
- **JWT** + sesión en el servidor: se invalida al cerrar sesión, al dar de baja la tienda y tras **15 minutos sin actividad**.
- Roles `ADMIN_PLATAFORMA`, `ADMIN_NEGOCIO`, `CLIENTE` y **aislamiento multi-tienda** (`id_tienda` en cada consulta; un intento de ver datos de otra tienda queda en la bitácora).
- **Bitácora inalterable**: la entidad es `@Immutable` y un trigger de PostgreSQL rechaza `UPDATE` y `DELETE`.
- **Consentimiento** (Ley 29733) con fecha, hora, IP y versión de la política; sin él no se registra al cliente.
- Topes del **BCRP** (TEA compensatoria 114.13 % y moratoria 17.12 %) validados al registrar o editar un cliente.
- Cabeceras de seguridad (CSP, HSTS, Referrer-Policy) y CORS restringido.

### Endpoints principales

| Pantalla | Método y ruta |
|----------|---------------|
| P01-P02 | `POST /api/auth/captcha`, `POST /api/auth/login`, `POST /api/auth/mfa/verificar`, `POST /api/auth/mfa/reenviar`, `GET /api/auth/yo`, `POST /api/auth/logout` |
| P03 | `GET/POST /api/admin/tiendas`, `PUT /api/admin/tiendas/{id}`, `PATCH .../baja`, `PATCH .../reactivar`, `GET/PUT /api/admin/topes` |
| P04 | `GET /api/panel` |
| P05 | `GET/POST /api/productos`, `PUT /api/productos/{id}`, `PATCH .../baja`, `PATCH .../reactivar`, `POST /api/productos/imagen` |
| P06 | `GET/POST /api/clientes`, `GET/PUT /api/clientes/{id}`, `PATCH .../baja`, `PATCH .../reactivar` |
| P07 | `POST /api/compras/simular`, `POST /api/compras` |
| P08 | `GET /api/compras/{id}/cronograma`, `GET /api/compras/{id}/cronograma/pdf` |
| P09 | `GET /api/cortes?anio=&mes=`, `POST /api/cortes/generar`, `GET /api/estados-cuenta/{id}/pdf` |
| P10 | `GET /api/pagos/preview?idCliente=&fecha=`, `POST /api/pagos` |
| P11 | `GET /api/mi-cuenta`, `GET /api/mi-cuenta/estado-cuenta/pdf`, `GET /api/mi-cuenta/compras/{id}/cronograma(/pdf)` |
| P12 | `GET /api/bitacora?usuario=&accion=&ip=&desde=&hasta=`, `GET /api/bitacora/acciones` |

### Pruebas

```bash
cd backend
./mvnw test          # reporte de cobertura en target/site/jacoco/index.html
```

`MotorFinancieroTest` comprueba al céntimo los juegos de prueba del informe (sección 6.5):

| Caso | Esperado | Obtenido |
|------|----------|----------|
| Compra 03/09 a fin de mes (CP05) | FP 26/09, S = 121.66 | ✔ |
| Compra 22/09 después del corte (CP06) | FP 26/10, S = 61.23 | ✔ |
| Cocina en 3 cuotas (CP07) | C' = 452.97, R = 156.48, intereses 8.19 / 5.51 / 2.78, T = 469.45 | ✔ |
| Tasa nominal TNA 36 % (CP08) | TEM = 0.0304391, R = 160.85 | ✔ |
| Pago con 3 días de mora (CP09) | 0.37 + 0.24, total 208.18 | ✔ |
| Pago con 4 días de mora (juego 2) | 0.80 + 0.33, total 201.69 | ✔ |
| Tope moratorio (CP13) | TNA 18 % cap. diaria = TEA 19.72 % | ✔ |
