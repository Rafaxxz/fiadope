import { Component, computed, inject, input, signal } from '@angular/core';
import { ApiService } from '../core/api.service';

const ICONOS: Record<string, string> = {
  'Abarrotes y granel': 'grocery',
  'Lácteos y panadería': 'bakery_dining',
  'Bebidas y gaseosas': 'local_drink',
  'Electro y hogar': 'kitchen',
  'Otro': 'inventory_2',
};

/** Foto del producto con imagen genérica si no tiene o no carga. */
@Component({
  selector: 'fp-producto-img',
  standalone: true,
  template: `
    @if (src() && !fallo()) {
      <img [src]="src()" [alt]="alt()" class="w-full h-full object-cover" (error)="fallo.set(true)" loading="lazy" />
    } @else {
      <div class="w-full h-full flex items-center justify-center bg-gradient-to-br from-primary-fixed via-secondary-fixed to-tertiary-fixed">
        <span class="material-symbols-outlined text-on-primary-fixed-variant" [style.font-size.px]="tamano()">{{ icono() }}</span>
      </div>
    }`,
  host: { class: 'block overflow-hidden' },
})
export class ProductoImg {
  private api = inject(ApiService);
  url = input<string | null>(null);
  categoria = input<string>('Otro');
  alt = input<string>('Producto');
  tamano = input<number>(48);
  fallo = signal(false);
  src = computed(() => this.api.imagen(this.url()));
  icono = computed(() => ICONOS[this.categoria()] ?? 'inventory_2');
}
