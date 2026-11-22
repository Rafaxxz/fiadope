import { Injectable, signal } from '@angular/core';

const CLAVE = 'fiadope.tema';

/** Tema claro/oscuro. Por defecto sigue al sistema; si el usuario elige uno, se recuerda en este navegador. */
@Injectable({ providedIn: 'root' })
export class TemaService {
  readonly oscuro = signal(document.documentElement.classList.contains('dark'));

  alternar() { this.aplicar(!this.oscuro(), true); }

  private aplicar(oscuro: boolean, guardar: boolean) {
    const html = document.documentElement;
    // Cambio suave de colores solo durante el cambio de tema (no en cada hover)
    html.classList.add('fp-cambiando-tema');
    html.classList.toggle('dark', oscuro);
    document.querySelector('meta[name="theme-color"]')?.setAttribute('content', oscuro ? '#1f1d1d' : '#e6007e');
    this.oscuro.set(oscuro);
    if (guardar) { try { localStorage.setItem(CLAVE, oscuro ? 'oscuro' : 'claro'); } catch { /* sin almacenamiento */ } }
    setTimeout(() => html.classList.remove('fp-cambiando-tema'), 400);
  }
}
