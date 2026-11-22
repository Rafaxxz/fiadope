import { Component, inject } from '@angular/core';
import { TemaService } from '../core/tema.service';

/** Botón sol/luna para cambiar entre modo claro y oscuro. */
@Component({
  selector: 'fp-boton-tema',
  standalone: true,
  template: `
    <button type="button" (click)="tema.alternar()" [attr.aria-label]="tema.oscuro() ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro'"
            [title]="tema.oscuro() ? 'Modo claro' : 'Modo oscuro'"
            class="relative inline-flex items-center justify-center w-11 h-11 rounded-xl bg-surface-container hover:bg-surface-container-high text-on-surface transition-colors overflow-hidden">
      <span class="material-symbols-outlined text-[1.4rem] absolute transition-all duration-500" [class]="tema.oscuro() ? 'opacity-0 rotate-90 scale-50' : 'opacity-100 rotate-0 scale-100 text-secondary'">light_mode</span>
      <span class="material-symbols-outlined text-[1.4rem] absolute transition-all duration-500" [class]="tema.oscuro() ? 'opacity-100 rotate-0 scale-100 text-tertiary' : 'opacity-0 -rotate-90 scale-50'">dark_mode</span>
    </button>`,
})
export class BotonTema {
  tema = inject(TemaService);
}
