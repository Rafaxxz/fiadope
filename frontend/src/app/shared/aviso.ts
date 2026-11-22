import { Component, Injectable, inject, signal } from '@angular/core';

export interface Aviso { id: number; tipo: 'ok' | 'error' | 'info'; texto: string; saliendo?: boolean; }

const DURACION = 5000;

/**
 * Avisos flotantes (reemplazan los alert() de los mockups). Se cierran solos a los 5 s;
 * una barra muestra el tiempo que queda y el conteo se pausa mientras el mouse está encima.
 */
@Injectable({ providedIn: 'root' })
export class AvisoService {
  readonly lista = signal<Aviso[]>([]);
  readonly duracion = DURACION;
  private n = 0;
  private relojes = new Map<number, { restante: number; inicio: number; t: ReturnType<typeof setTimeout> | null }>();

  ok(texto: string) { this.mostrar('ok', texto); }
  error(texto: string) { this.mostrar('error', texto); }
  info(texto: string) { this.mostrar('info', texto); }

  private mostrar(tipo: Aviso['tipo'], texto: string) {
    const id = ++this.n;
    this.lista.update((l) => [...l.slice(-3), { id, tipo, texto }]);
    this.relojes.set(id, { restante: DURACION, inicio: Date.now(), t: null });
    this.reanudar(id);
  }

  pausar(id: number) {
    const r = this.relojes.get(id);
    if (!r || !r.t) return;
    clearTimeout(r.t); r.t = null;
    r.restante -= Date.now() - r.inicio;
  }

  reanudar(id: number) {
    const r = this.relojes.get(id);
    if (!r || r.t) return;
    r.inicio = Date.now();
    r.t = setTimeout(() => this.quitar(id), Math.max(0, r.restante));
  }

  /** Marca el aviso como «saliendo» para animar su salida y luego lo quita. */
  quitar(id: number) {
    const r = this.relojes.get(id);
    if (r?.t) clearTimeout(r.t);
    this.relojes.delete(id);
    this.lista.update((l) => l.map((a) => (a.id === id ? { ...a, saliendo: true } : a)));
    setTimeout(() => this.lista.update((l) => l.filter((a) => a.id !== id)), 260);
  }
}

@Component({
  selector: 'fp-avisos',
  standalone: true,
  template: `
    <div class="fixed bottom-4 right-4 z-[70] flex flex-col gap-2 max-w-sm w-[calc(100%-2rem)]" aria-live="polite">
      @for (a of avisos.lista(); track a.id) {
        <div class="fp-aviso group relative overflow-hidden flex items-start gap-2 p-3 pb-4 rounded-lg shadow-[3px_4px_0px_#1c1b1b] font-body-sm text-body-sm"
             [class.fp-aviso-sale]="a.saliendo" [attr.role]="a.tipo === 'error' ? 'alert' : 'status'"
             [class]="a.tipo === 'ok' ? 'bg-[#00C853] text-[#111111]' : a.tipo === 'error' ? 'bg-error text-on-error' : 'bg-inverse-surface text-inverse-on-surface'"
             (mouseenter)="avisos.pausar(a.id)" (mouseleave)="avisos.reanudar(a.id)" (focusin)="avisos.pausar(a.id)" (focusout)="avisos.reanudar(a.id)">
          <span class="material-symbols-outlined text-lg">{{ a.tipo === 'ok' ? 'check_circle' : a.tipo === 'error' ? 'error' : 'info' }}</span>
          <span class="flex-1 font-semibold">{{ a.texto }}</span>
          <button type="button" class="opacity-80 hover:opacity-100" aria-label="Cerrar aviso" (click)="avisos.quitar(a.id)"><span class="material-symbols-outlined text-lg">close</span></button>
          <span class="fp-aviso-tiempo absolute left-0 bottom-0 h-1 w-full bg-current opacity-40 group-hover:[animation-play-state:paused] group-focus-within:[animation-play-state:paused]"
                [style.animation-duration.ms]="avisos.duracion"></span>
        </div>
      }
    </div>`,
})
export class Avisos {
  avisos = inject(AvisoService);
}
