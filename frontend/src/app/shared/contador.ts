import { Component, DestroyRef, effect, inject, input, signal } from '@angular/core';
import { soles } from './formato';

/**
 * Muestra un monto en soles que «cuenta» desde el valor anterior hasta el nuevo (desde 0 la primera vez).
 * Usa requestAnimationFrame con una curva que frena al final; con «reducir movimiento» salta directo al valor.
 */
@Component({
  selector: 'fp-contador',
  standalone: true,
  host: { class: 'tabular-nums' },
  template: `{{ texto() }}`,
})
export class Contador {
  valor = input<number | null | undefined>(0);
  decimales = input(2);
  duracion = input(900);
  texto = signal(this.formatear(0));
  private actual = 0;
  private raf = 0;

  constructor() {
    inject(DestroyRef).onDestroy(() => cancelAnimationFrame(this.raf));
    effect(() => this.animar(Number(this.valor() ?? 0)));
  }

  private formatear(v: number) {
    return this.decimales() === 2 ? soles(v) : v.toFixed(this.decimales());
  }

  private animar(destino: number) {
    cancelAnimationFrame(this.raf);
    const quieto = typeof matchMedia !== 'undefined' && matchMedia('(prefers-reduced-motion: reduce)').matches;
    const desde = this.actual;
    if (quieto || desde === destino) { this.actual = destino; this.texto.set(this.formatear(destino)); return; }
    const inicio = performance.now();
    const paso = (t: number) => {
      const p = Math.min(1, (t - inicio) / this.duracion());
      const e = 1 - Math.pow(1 - p, 4);
      this.actual = desde + (destino - desde) * e;
      this.texto.set(this.formatear(p === 1 ? destino : this.actual));
      if (p < 1) this.raf = requestAnimationFrame(paso);
      else this.actual = destino;
    };
    this.raf = requestAnimationFrame(paso);
  }
}
