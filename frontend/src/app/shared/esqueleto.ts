import { Component, computed, input } from '@angular/core';

/**
 * Bloques grises con brillo que ocupan el lugar del contenido mientras llegan los datos.
 * tipo «fila»: una fila de tabla (avatar + textos + montos); «tarjetas»: n tarjetas en grilla;
 * «lista»: n renglones; «bloque»: un bloque del alto indicado.
 */
@Component({
  selector: 'fp-esqueleto',
  standalone: true,
  host: { 'aria-hidden': 'true', class: 'block' },
  template: `
    @switch (tipo()) {
      @case ('fila') {
        @for (i of items(); track i) {
          <div class="flex items-center gap-space-md py-3" [style.opacity]="1 - i * 0.12">
            <div class="fp-skel w-10 h-10 rounded-full shrink-0"></div>
            <div class="flex-1 flex flex-col gap-2 min-w-0">
              <div class="fp-skel h-4 rounded w-2/5"></div>
              <div class="fp-skel h-3 rounded w-3/5"></div>
            </div>
            <div class="hidden sm:flex flex-col gap-2 items-end w-28"><div class="fp-skel h-4 rounded w-full"></div><div class="fp-skel h-3 rounded w-2/3"></div></div>
            <div class="hidden md:block fp-skel h-7 rounded-full w-24"></div>
            <div class="hidden lg:block fp-skel h-8 rounded-lg w-20"></div>
          </div>
        }
      }
      @case ('tarjetas') {
        <div class="grid gap-space-lg" [class]="columnas()">
          @for (i of items(); track i) {
            <div class="bg-surface-container-lowest rounded-xl p-space-lg shadow-sm flex flex-col gap-3">
              @if (imagen()) { <div class="fp-skel h-40 rounded-lg w-full"></div> }
              <div class="fp-skel h-3 rounded w-1/3"></div>
              <div class="fp-skel h-8 rounded w-2/3"></div>
              <div class="fp-skel h-3 rounded w-4/5"></div>
              <div class="fp-skel h-10 rounded-lg w-full mt-2"></div>
            </div>
          }
        </div>
      }
      @case ('lista') {
        <div class="flex flex-col gap-2">
          @for (i of items(); track i) { <div class="fp-skel h-10 rounded-lg w-full" [style.opacity]="1 - i * 0.1"></div> }
        </div>
      }
      @default { <div class="fp-skel rounded-xl w-full" [style.height]="alto()"></div> }
    }
    <span class="sr-only">Cargando…</span>`,
})
export class Esqueleto {
  tipo = input<'fila' | 'tarjetas' | 'lista' | 'bloque'>('fila');
  n = input(3);
  alto = input('10rem');
  imagen = input(false);
  columnas = input('grid-cols-1 md:grid-cols-3');
  items = computed(() => Array.from({ length: this.n() }, (_, i) => i));
}
