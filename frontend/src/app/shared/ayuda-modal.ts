import { Component, input, output } from '@angular/core';

export interface PasoAyuda { titulo: string; texto: string; }

/** Ventana «¿Cómo funciona?» con los pasos de la pantalla (manual de usuario en línea). */
@Component({
  selector: 'fp-ayuda',
  standalone: true,
  template: `
    <div class="fixed inset-0 bg-inverse-surface/60 backdrop-blur-sm z-[60] flex items-center justify-center p-space-md" (click)="cerrar.emit()">
      <div class="relative w-full max-w-lg bg-surface-container-lowest rounded-xl shadow-[6px_8px_0px_#1c1b1b] overflow-hidden flex flex-col max-h-[90vh]" (click)="$event.stopPropagation()">
        <div class="h-2 w-full bg-gradient-to-r from-primary-container via-secondary-container to-tertiary-fixed"></div>
        <div class="p-space-lg flex items-center justify-between bg-surface-container-low">
          <div class="flex items-center gap-2">
            <div class="w-8 h-8 rounded-full bg-tertiary-fixed text-on-tertiary-fixed flex items-center justify-center font-headline-sm text-headline-sm shadow-[1px_2px_0px_#1c1b1b]">?</div>
            <div>
              <h2 class="font-headline-sm text-headline-sm text-on-surface uppercase">{{ titulo() }}</h2>
              @if (subtitulo()) { <p class="font-body-sm text-body-sm text-on-surface-variant">{{ subtitulo() }}</p> }
            </div>
          </div>
          <button aria-label="Cerrar ventana" class="p-2 text-on-surface-variant hover:text-on-surface rounded-lg hover:bg-surface-container transition-colors" type="button" (click)="cerrar.emit()">
            <span class="material-symbols-outlined text-2xl">close</span>
          </button>
        </div>
        <div class="p-space-lg overflow-y-auto flex flex-col gap-space-md">
          @for (p of pasos(); track $index) {
            <div class="flex items-start gap-space-md p-space-md rounded-xl bg-surface-container-low">
              <div class="flex-shrink-0 w-10 h-10 rounded-lg flex items-center justify-center font-headline-sm text-headline-sm shadow-[2px_2px_0px_#1c1b1b]"
                   [class]="colores[$index % 3]">{{ $index + 1 }}</div>
              <div class="flex-1">
                <h3 class="font-headline-sm text-headline-sm text-on-surface">{{ p.titulo }}</h3>
                <p class="font-body-sm text-body-sm text-on-surface-variant mt-1">{{ p.texto }}</p>
              </div>
            </div>
          }
          @if (nota()) {
            <div class="p-3 bg-primary-fixed/50 rounded-lg flex items-center gap-2">
              <span class="material-symbols-outlined text-primary text-xl">tips_and_updates</span>
              <span class="font-body-sm text-body-sm text-on-surface">{{ nota() }}</span>
            </div>
          }
        </div>
        <div class="p-space-md bg-surface-container-low flex justify-end">
          <button class="px-5 py-2.5 bg-on-surface text-surface font-headline-sm text-headline-sm uppercase rounded-lg shadow-[2px_3px_0px_#e6007e]" type="button" (click)="cerrar.emit()">
            {{ boton() }}
          </button>
        </div>
      </div>
    </div>`,
})
export class AyudaModal {
  titulo = input('¿Cómo funciona?');
  subtitulo = input<string>('');
  pasos = input.required<PasoAyuda[]>();
  nota = input<string>('');
  boton = input('¡Entendido!');
  cerrar = output<void>();
  colores = [
    'bg-primary-container text-on-primary-container',
    'bg-secondary-container text-on-secondary',
    'bg-tertiary-fixed text-on-tertiary-fixed',
  ];
}

