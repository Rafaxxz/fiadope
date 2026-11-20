import { inject, Injectable, NgZone, signal, computed } from '@angular/core';
import { Router } from '@angular/router';
import { ApiService } from './api.service';
import { LoginRespuesta, Perfil, Rol, SesionRespuesta } from './models';

const CLAVE_TOKEN = 'fiadope.token';
const CLAVE_PERFIL = 'fiadope.perfil';
const CLAVE_MFA = 'fiadope.mfa';
const INACTIVIDAD_MS = 15 * 60 * 1000;

/**
 * Sesión del usuario. El token vive en sessionStorage (se borra al cerrar la pestaña) y la sesión se cierra
 * tras 15 minutos sin actividad (MIN_INACTIVIDAD); el servidor aplica la misma regla.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private api = inject(ApiService);
  private router = inject(Router);
  private zone = inject(NgZone);

  readonly perfil = signal<Perfil | null>(this.leer<Perfil>(CLAVE_PERFIL));
  readonly rol = computed(() => this.perfil()?.rol ?? null);
  readonly mensajeSalida = signal<string | null>(null);
  private temporizador: ReturnType<typeof setTimeout> | null = null;
  private ultimaActividad = Date.now();

  constructor() {
    if (this.token) this.vigilarInactividad();
  }

  get token(): string | null {
    try { return sessionStorage.getItem(CLAVE_TOKEN); } catch { return null; }
  }

  /** Datos del paso MFA pendiente (entre P01 y P02). */
  get mfa(): (LoginRespuesta & { usuario: string }) | null { return this.leer(CLAVE_MFA); }
  guardarMfa(r: LoginRespuesta, usuario: string) { this.escribir(CLAVE_MFA, { ...r, usuario }); }

  iniciar(s: SesionRespuesta) {
    try {
      sessionStorage.setItem(CLAVE_TOKEN, s.token);
      sessionStorage.removeItem(CLAVE_MFA);
    } catch { /* almacenamiento no disponible */ }
    this.escribir(CLAVE_PERFIL, s.perfil);
    this.perfil.set(s.perfil);
    this.mensajeSalida.set(null);
    this.vigilarInactividad();
  }

  inicio(rol: Rol | null = this.rol()): string {
    switch (rol) {
      case 'ADMIN_PLATAFORMA': return '/admin/tiendas';
      case 'CLIENTE': return '/mi-cuenta';
      case 'ADMIN_NEGOCIO': return '/panel';
      default: return '/login';
    }
  }

  cerrarSesion(motivo?: string, avisarServidor = true) {
    if (avisarServidor && this.token) this.api.logout().subscribe({ error: () => undefined });
    this.limpiar();
    this.mensajeSalida.set(motivo ?? null);
    // replaceUrl: el botón «Atrás» no regresa a la pantalla protegida (HU01 CA4)
    this.router.navigate(['/login'], { replaceUrl: true });
  }

  limpiar() {
    try {
      sessionStorage.removeItem(CLAVE_TOKEN);
      sessionStorage.removeItem(CLAVE_PERFIL);
      sessionStorage.removeItem(CLAVE_MFA);
    } catch { /* nada */ }
    this.perfil.set(null);
    if (this.temporizador) clearTimeout(this.temporizador);
  }

  private vigilarInactividad() {
    const registrar = () => { this.ultimaActividad = Date.now(); };
    this.zone.runOutsideAngular(() => {
      ['click', 'keydown', 'mousemove', 'touchstart', 'scroll'].forEach((e) =>
        window.addEventListener(e, registrar, { passive: true }));
    });
    const revisar = () => {
      if (!this.token) return;
      if (Date.now() - this.ultimaActividad >= INACTIVIDAD_MS) {
        this.zone.run(() => this.cerrarSesion('Tu sesión se cerró por 15 minutos sin actividad.'));
        return;
      }
      this.temporizador = setTimeout(revisar, 30_000);
    };
    if (this.temporizador) clearTimeout(this.temporizador);
    this.temporizador = setTimeout(revisar, 30_000);
  }

  private leer<T>(k: string): T | null {
    try { const v = sessionStorage.getItem(k); return v ? JSON.parse(v) as T : null; } catch { return null; }
  }
  private escribir(k: string, v: unknown) {
    try { sessionStorage.setItem(k, JSON.stringify(v)); } catch { /* nada */ }
  }
}

