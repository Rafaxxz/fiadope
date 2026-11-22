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
    <header style="view-transition-name: cabecera" class="fixed top-0 left-0 right-0 z-50 bg-surface-container-lowest shadow-[0_4px_16px_rgba(0,0,0,0.06)]">
      <div class="h-[5.5rem] w-full px-gutter lg:px-gutter-desktop max-w-[88rem] mx-auto flex items-center justify-between gap-space-md">
        <a [routerLink]="auth.inicio()" class="flex items-center gap-space-sm shrink-0" aria-label="Ir al inicio">
          <img alt="Logo FiadoPe" class="h-10 sm:h-12 w-auto object-contain" src="assets/img/logo-fiadope.png" />
        </a>
        <nav class="hidden xl:flex flex-1 min-w-0 overflow-x-auto scroll-fino py-1.5">
          <div class="flex items-center gap-1 w-max mx-auto">
          @for (e of enlaces(); track e.ruta) {
            <a [routerLink]="e.ruta" routerLinkActive="!bg-primary-container !text-on-primary-container shadow-[2px_3px_0px_#1c1b1b]"
               class="font-label-lg text-label-md font-semibold text-on-surface-variant hover:bg-surface-container hover:text-on-surface px-3 py-2 rounded-lg transition-colors whitespace-nowrap">{{ e.texto }}</a>
          }
          </div>
        </nav>
        <div class="flex items-center gap-space-sm shrink-0">
          <button class="inline-flex items-center justify-center w-11 h-11 rounded-xl bg-surface-container hover:bg-surface-container-high text-on-surface transition-colors" title="Guía rápida: ¿cómo funciona?" aria-label="Guía rápida de uso" type="button" (click)="ayuda.set(true)">
            <span class="material-symbols-outlined text-[1.5rem] text-primary">help</span>
          </button>
          <div class="h-8 w-px bg-outline-variant/50 hidden sm:block"></div>
          <div class="relative">
            <button type="button" class="flex items-center gap-space-sm pl-space-xs rounded-xl hover:bg-surface-container-low py-1 pr-1 transition-colors" (click)="menu.set(!menu())" aria-label="Menú de usuario">
              <div class="hidden sm:flex xl:hidden min-[1700px]:flex flex-col text-right">
                <span class="font-label-lg text-label-lg font-bold text-on-surface leading-tight">{{ nombreCorto() }}</span>
                <span class="font-body-sm text-[0.8125rem] text-on-surface-variant">{{ rolTexto() }}</span>
              </div>
              <span class="w-11 h-11 rounded-full ring-2 ring-primary-container bg-primary-fixed text-on-primary-fixed font-headline-sm text-[1rem] flex items-center justify-center">{{ iniciales() }}</span>
              <span class="material-symbols-outlined text-[1.25rem] text-on-surface-variant transition-transform hidden sm:inline xl:hidden min-[1700px]:inline" [class.rotate-180]="menu()">expand_more</span>
            </button>
            @if (menu()) {
              <div class="fp-desplegar absolute right-0 top-[3.75rem] w-72 bg-surface-container-lowest rounded-xl shadow-[3px_4px_0px_#1c1b1b] overflow-hidden z-50">
                <div class="h-1.5 chicha-gradient"></div>
                <div class="p-4 border-b border-outline-variant/30">
                  <p class="font-label-lg text-label-lg text-on-surface">{{ perfil()?.nombre }}</p>
                  <p class="font-body-sm text-body-sm text-on-surface-variant break-all">{{ perfil()?.login }} · {{ perfil()?.correo }}</p>
                  @if (perfil()?.tienda; as t) {
                    <p class="mt-2 inline-flex items-center gap-1 font-label-md text-label-md bg-tertiary-fixed/40 text-on-tertiary-fixed px-2 py-1 rounded-full">🏪 {{ t.razonSocial }}</p>
                  }
                </div>
                <button type="button" class="w-full flex items-center gap-2 px-4 py-3.5 text-error font-label-lg text-label-lg hover:bg-error-container/40" (click)="salir()">
                  <span class="material-symbols-outlined">logout</span> Cerrar sesión
                </button>
              </div>
            }
          </div>
          <button type="button" class="xl:hidden inline-flex items-center justify-center w-11 h-11 rounded-xl bg-surface-container" (click)="movil.set(!movil())" aria-label="Abrir menú">
            <span class="material-symbols-outlined">{{ movil() ? 'close' : 'menu' }}</span>
          </button>
        </div>
      </div>
      <div class="h-1.5 w-full chicha-gradient"></div>
      @if (movil()) {
        <nav class="fp-desplegar xl:hidden bg-surface-container-lowest px-gutter py-3 grid grid-cols-2 sm:grid-cols-4 gap-1.5 shadow-md">
          @for (e of enlaces(); track e.ruta) {
            <a [routerLink]="e.ruta" (click)="movil.set(false)" routerLinkActive="!bg-primary-container !text-on-primary-container font-bold"
               class="font-label-lg text-label-lg text-on-surface-variant px-3 py-3 rounded-lg bg-surface-container-low hover:bg-surface-container">{{ e.texto }}</a>
          }
        </nav>
      }
    </header>
    <main class="w-full pt-[5.875rem] bg-surface min-h-[calc(100vh-5.5rem)]">
      <router-outlet />
    </main>
    <footer style="view-transition-name: pie" class="w-full bg-surface-container-low border-t border-outline-variant/30 py-space-xl mt-space-xl">
      <div class="max-w-[88rem] mx-auto px-gutter lg:px-gutter-desktop flex flex-col md:flex-row items-center justify-between gap-space-md text-center md:text-left">
        <div class="flex flex-col items-center md:items-start gap-1">
          <div class="flex items-center gap-space-sm">
            <img alt="FiadoPe" class="h-9 w-auto object-contain" src="assets/img/logo-fiadope.png" />
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

