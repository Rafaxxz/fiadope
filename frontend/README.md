# FiadoPe · Frontend (Angular)

SPA en Angular 20 con Tailwind CSS 3, usando **la misma configuración de colores, tipografías (Rubik + Inter), radios y
espaciados de los mockups de Stitch** (`tailwind.config.js`).

```bash
npm install
npx ng serve          # http://localhost:4200  (usa la API en http://localhost:8080)
npx ng build          # producción en dist/fiadope-web/browser
```

- En desarrollo la API se toma de `src/environments/environment.development.ts`.
- En producción la API se llama en el mismo dominio (`/api`), detrás de nginx (Dockerfile) o de los rewrites de Vercel (`vercel.json`).

| Pantalla | Ruta | Rol |
|----------|------|-----|
| P01 Login | `/login` | Todos |
| P02 Verificación MFA | `/verificar` | Todos |
| P03 Gestión de tiendas | `/admin/tiendas` | Admin. plataforma |
| P04 Panel del negocio | `/panel` | Admin. negocio |
| P05 Catálogo | `/catalogo` | Admin. negocio |
| P06 Ficha de cliente | `/clientes`, `/clientes/nuevo`, `/clientes/:id` | Admin. negocio |
| P07 Nueva compra | `/nueva-compra` | Admin. negocio |
| P08 Cronograma | `/cronograma/:id` | Admin. negocio |
| P09 Corte del mes | `/corte` | Admin. negocio |
| P10 Registrar pago | `/pagos` | Admin. negocio |
| P11 Mi cuenta | `/mi-cuenta` | Cliente |
| P12 Bitácora | `/bitacora` | Admin. negocio / plataforma |

Cada campo de captura tiene su ícono ⓘ con la ayuda en línea y cada pantalla su botón «¿Cómo funciona?».
La sesión se cierra tras 15 minutos sin actividad y al cerrar sesión no se puede volver con «Atrás».
