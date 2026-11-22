import { Component, Injectable, inject, signal } from '@angular/core';

export interface Aviso { id: number; tipo: 'ok' | 'error' | 'info'; texto: string; }

/** Avisos flotantes (reemplazan los alert() de los mockups). */
@Injectable({ providedIn: 'root' })
export class AvisoService {
  readonly lista = signal<Aviso[]>([]);
  private n = 0;
  ok(texto: string) { this.mostrar('ok', texto); }
  error(texto: string) { this.mostrar('error', texto); }
  info(texto: string) { this.mostrar('info', texto); }
  private mostrar(tipo: Aviso['tipo'], texto: string) {
    const id = ++this.n;
    this.lista.update((l) => [...l, { id, tipo, texto }]);
    setTimeout(() => this.quitar(id), 5000);
  }
  quitar(id: number) { this.lista.update((l) => l.filter((a) => a.id !== id)); }
}

@Component({
  selector: 'fp-avisos',
  standalone: true,
  template: `
    <div class="fixed bottom-4 right-4 z-[70] flex flex-col gap-2 max-w-sm w-[calc(100%-2rem)]" aria-live="polite">
      @for (a of avisos.lista(); track a.id) {
        <div class="fp-aviso flex items-start gap-2 p-3 rounded-lg shadow-[3px_4px_0px_#1c1b1b] font-body-sm text-body-sm"
             [class]="a.tipo === 'ok' ? 'bg-[#00C853] text-[#111111]' : a.tipo === 'error' ? 'bg-error text-on-error' : 'bg-inverse-surface text-inverse-on-surface'">
          <span class="material-symbols-outlined text-lg">{{ a.tipo === 'ok' ? 'check_circle' : a.tipo === 'error' ? 'error' : 'info' }}</span>
          <span class="flex-1 font-semibold">{{ a.texto }}</span>
          <button type="button" class="opacity-80 hover:opacity-100" (click)="avisos.quitar(a.id)"><span class="material-symbols-outlined text-lg">close</span></button>
        </div>
      }
    </div>`,
})
export class Avisos {
  avisos = inject(AvisoService);
}

