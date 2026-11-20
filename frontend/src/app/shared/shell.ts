import { Component, computed, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { AuthService } from '../core/auth.service';
import { AyudaModal } from './ayuda-modal';
import { AYUDAS } from './ayudas';

interface Enlace { ruta: string; texto: string; }

/** Cabecera, navegación por rol y pie de página de los mockups P03-P12. */
@Component({
  selector: 'fp-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, AyudaModal],
  template: `
    <header class="fixed top-0 left-0 right-0 z-50 bg-surface-container-lowest shadow-[0_4px_16px_rgba(0,0,0,0.06)]">
      <div class="h-20 w-full px-gutter lg:px-gutter-desktop max-w-[1360px] mx-auto flex items-center justify-between gap-space-md">
        <div class="flex items-center gap-space-md shrink-0">
          <a [routerLink]="auth.inicio()" class="flex items-center gap-space-sm">
            <img alt="Logo FiadoPe" class="h-8 w-auto object-contain" src="assets/img/logo-fiadope.png" />
            <div class="flex flex-col">
              <span class="font-headline-sm text-headline-sm text-primary uppercase leading-tight tracking-tight">FiadoPe</span>
              <span class="font-label-badge text-label-badge text-on-surface-variant tracking-wider uppercase">Libreta Digital</span>
            </div>
          </a>
          @if (perfil()?.tienda; as t) {
            <div class="hidden 2xl:flex items-center gap-space-xs bg-tertiary-fixed/30 px-space-sm py-1 rounded-full border border-tertiary-fixed-dim/50">
              <span class="font-label-md text-label-md text-on-tertiary-fixed">🏪 {{ t.razonSocial }}</span>
              <span class="font-label-badge text-label-badge px-1.5 py-0.5 rounded-full bg-secondary-container text-on-secondary">{{ perfil()?.rol === 'CLIENTE' ? 'Vecino' : 'Caja 01' }}</span>
            </div>
          } @else {
            <div class="hidden 2xl:flex items-center gap-space-xs bg-primary-fixed/40 px-space-sm py-1 rounded-full border border-primary-fixed-dim/50">
              <span class="font-label-md text-label-md text-on-primary-fixed-variant">🛡️ Plataforma FiadoPe</span>
            </div>
          }
        </div>
        <nav class="hidden lg:flex items-center gap-1 overflow-x-auto py-1">
          @for (e of enlaces(); track e.ruta) {
            <a [routerLink]="e.ruta" routerLinkActive="bg-primary-container text-on-primary-container shadow-sm font-bold !text-on-primary-container"
               class="font-label-md text-label-md text-on-surface-variant hover:bg-surface-container hover:text-on-surface px-2.5 py-2 rounded-lg transition-colors whitespace-nowrap">{{ e.texto }}</a>
          }
        </nav>
        <div class="flex items-center gap-space-sm shrink-0">
          <button class="inline-flex items-center gap-1.5 px-space-sm py-2 rounded-lg bg-surface-container hover:bg-surface-container-high text-on-surface font-label-md text-label-md transition-colors" title="Guía rápida de uso" type="button" (click)="ayuda.set(true)">
            <span class="material-symbols-outlined text-[18px] text-primary">help</span><span class="hidden 2xl:inline">¿Cómo funciona?</span>
          </button>
          <div class="h-6 w-px bg-outline-variant/50 hidden sm:block"></div>
          <div class="relative">
            <button type="button" class="flex items-center gap-space-sm pl-space-xs" (click)="menu.set(!menu())" aria-label="Menú de usuario">
              <div class="hidden sm:flex flex-col text-right">
                <span class="font-label-md text-label-md text-on-surface leading-tight">{{ nombreCorto() }}</span>
                <span class="font-body-sm text-[11px] text-on-surface-variant">{{ rolTexto() }}</span>
              </div>
              <span class="w-8 h-8 rounded-full ring-2 ring-primary-container bg-primary-fixed text-on-primary-fixed font-headline-sm text-[13px] flex items-center justify-center">{{ iniciales() }}</span>
            </button>
            @if (menu()) {
              <div class="absolute right-0 top-11 w-64 bg-surface-container-lowest rounded-xl shadow-[3px_4px_0px_#1c1b1b] overflow-hidden z-50">
                <div class="h-1.5 chicha-gradient"></div>
                <div class="p-3 border-b border-outline-variant/30">
                  <p class="font-label-lg text-label-lg text-on-surface">{{ perfil()?.nombre }}</p>
                  <p class="font-body-sm text-body-sm text-on-surface-variant">{{ perfil()?.login }} · {{ perfil()?.correo }}</p>
                </div>
                <button type="button" class="w-full flex items-center gap-2 px-3 py-3 text-error font-label-lg text-label-lg hover:bg-error-container/40" (click)="salir()">
                  <span class="material-symbols-outlined">logout</span> Cerrar sesión
                </button>
              </div>
            }
          </div>
          <button type="button" class="lg:hidden p-2 rounded-lg bg-surface-container" (click)="movil.set(!movil())" aria-label="Abrir menú">
            <span class="material-symbols-outlined">{{ movil() ? 'close' : 'menu' }}</span>
          </button>
        </div>
      </div>
      <div class="h-1.5 w-full chicha-gradient"></div>
      @if (movil()) {
        <nav class="lg:hidden bg-surface-container-lowest px-gutter py-2 grid grid-cols-2 gap-1 shadow-md">
          @for (e of enlaces(); track e.ruta) {
            <a [routerLink]="e.ruta" (click)="movil.set(false)" routerLinkActive="bg-primary-container !text-on-primary-container font-bold"
               class="font-label-md text-label-md text-on-surface-variant px-3 py-2 rounded-lg hover:bg-surface-container">{{ e.texto }}</a>
          }
        </nav>
      }
    </header>
    <main class="w-full pt-[86px] bg-surface min-h-[calc(100vh-80px)]">
      <router-outlet />
    </main>
    <footer class="w-full bg-surface-container-low border-t border-outline-variant/30 py-space-xl mt-space-xl">
      <div class="max-w-[1360px] mx-auto px-gutter lg:px-gutter-desktop flex flex-col md:flex-row items-center justify-between gap-space-md text-center md:text-left">
        <div class="flex flex-col items-center md:items-start gap-1">
          <div class="flex items-center gap-space-sm">
            <span class="font-headline-sm text-headline-sm text-primary uppercase">FiadoPe</span>
            <span class="font-label-badge text-label-badge bg-tertiary-fixed text-on-tertiary-fixed px-2 py-0.5 rounded-full">Bodegas del Perú</span>
          </div>
          <p class="font-body-sm text-body-sm text-on-surface-variant">Deja el cuaderno, cobra con orden. Cuentas claras mantienen la confianza del barrio.</p>
        </div>
        <div class="flex flex-wrap justify-center gap-space-md text-on-surface-variant font-label-md text-label-md">
          <button type="button" class="hover:text-primary transition-colors" (click)="ayuda.set(true)">Preguntas Frecuentes</button>
          <a class="hover:text-primary transition-colors" href="https://wa.me/51900000000" target="_blank" rel="noopener">Soporte WhatsApp</a>
          <span class="text-outline-variant">•</span>
          <span class="font-body-sm text-body-sm text-on-surface-variant">© {{ anio }} FiadoPe Perú</span>
        </div>
      </div>
    </footer>
    @if (ayuda()) {
      <fp-ayuda [titulo]="ayudaActual().titulo" [pasos]="ayudaActual().pasos" [nota]="ayudaActual().nota ?? ''" (cerrar)="ayuda.set(false)" />
    }
  `,
})
export class Shell {
  auth = inject(AuthService);
  private router = inject(Router);
  perfil = this.auth.perfil;
  menu = signal(false);
  movil = signal(false);
  ayuda = signal(false);
  anio = new Date().getFullYear();
  private ruta = signal(this.router.url);

  constructor() {
    this.router.events.pipe(filter((e) => e instanceof NavigationEnd)).subscribe((e) => {
      this.ruta.set((e as NavigationEnd).urlAfterRedirects);
      this.menu.set(false);
    });
  }

  enlaces = computed<Enlace[]>(() => {
    switch (this.auth.rol()) {
      case 'ADMIN_PLATAFORMA':
        return [{ ruta: '/admin/tiendas', texto: 'Tiendas' }, { ruta: '/bitacora', texto: 'Bitácora' }];
      case 'CLIENTE':
        return [{ ruta: '/mi-cuenta', texto: 'Mi Libreta' }];
      default:
        return [
          { ruta: '/panel', texto: 'Panel' }, { ruta: '/clientes', texto: 'Clientes' }, { ruta: '/catalogo', texto: 'Catálogo' },
          { ruta: '/nueva-compra', texto: 'Nueva Compra' }, { ruta: '/cronograma', texto: 'Cronograma' },
          { ruta: '/corte', texto: 'Corte Mensual' }, { ruta: '/pagos', texto: 'Registrar Pago' }, { ruta: '/bitacora', texto: 'Bitácora' },
        ];
    }
  });

  ayudaActual = computed(() => {
    const r = this.ruta();
    const clave = r.startsWith('/admin') ? 'tiendas' : r.startsWith('/catalogo') ? 'catalogo' : r.startsWith('/clientes') ? 'clientes'
      : r.startsWith('/nueva-compra') ? 'compra' : r.startsWith('/cronograma') ? 'cronograma' : r.startsWith('/corte') ? 'corte'
      : r.startsWith('/pagos') ? 'pago' : r.startsWith('/mi-cuenta') ? 'micuenta' : r.startsWith('/bitacora') ? 'bitacora' : 'panel';
    return AYUDAS[clave];
  });

  nombreCorto = computed(() => {
    const p = (this.perfil()?.nombre ?? '').split(' ');
    return p.length > 1 ? `${p[0]} ${p[1][0]}.` : p[0];
  });
  iniciales = computed(() => (this.perfil()?.nombre ?? '?').split(' ').slice(0, 2).map((x) => x[0]).join(''));
  rolTexto = computed(() => ({ ADMIN_PLATAFORMA: 'Admin. plataforma', ADMIN_NEGOCIO: 'Administrador', CLIENTE: 'Cliente' })[this.auth.rol() ?? 'CLIENTE']);

  salir() { this.auth.cerrarSesion('Cerraste sesión correctamente.'); }
}

