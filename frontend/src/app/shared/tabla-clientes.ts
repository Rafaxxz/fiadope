import { Component, computed, input, output, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ClienteResumen } from '../core/models';
import { FechaPipe, InicialesPipe, SolesPipe } from './formato';

/** Tabla «Vecinos y cuentas corrientes» del panel (P04), reutilizada en Clientes. */
@Component({
  selector: 'fp-tabla-clientes',
  standalone: true,
  imports: [RouterLink, SolesPipe, FechaPipe, InicialesPipe],
  template: `
    <div class="bg-surface-container-lowest rounded-xl shadow-md overflow-hidden">
      <div class="overflow-x-auto scroll-fino">
        <table class="w-full text-left border-collapse min-w-[53.75rem]">
          <thead>
            <tr class="bg-surface-container text-on-surface-variant font-label-badge text-label-badge uppercase tracking-wider">
              <th class="whitespace-nowrap py-3 px-space-md">Vecino / Documento</th>
              <th class="whitespace-nowrap py-3 px-space-md text-right">Saldo al Corte</th>
              <th class="whitespace-nowrap py-3 px-space-md text-right">Crédito Libre</th>
              <th class="whitespace-nowrap py-3 px-space-md text-center">Estado Libreta</th>
              <th class="whitespace-nowrap py-3 px-space-md text-right">Acciones Rápidas</th>
            </tr>
          </thead>
          <tbody class="fp-escalonado font-body-sm text-body-sm text-on-surface">
            @for (c of visibles(); track c.id; let i = $index) {
              <tr class="transition-colors" [class]="c.enMora ? (i % 2 ? 'bg-error-container/10 hover:bg-error-container/20' : 'hover:bg-error-container/10') : (i % 2 ? 'bg-surface-container-low/40 hover:bg-surface-container-high' : 'hover:bg-surface-container-high')">
                <td class="py-4 px-space-md">
                  <div class="flex items-center gap-space-sm">
                    <div class="w-10 h-10 rounded-full flex items-center justify-center font-headline-sm font-bold text-headline-sm shrink-0"
                         [class]="c.enMora ? 'bg-error-container text-error' : (i % 2 ? 'bg-primary-fixed text-on-primary-fixed' : 'bg-secondary-fixed text-on-secondary-fixed')">{{ c.nombres | iniciales }}</div>
                    <div class="flex flex-col">
                      <a [routerLink]="['/clientes', c.id]" class="font-headline-sm text-headline-sm text-on-surface font-bold hover:text-primary">{{ c.nombres }}</a>
                      <span class="font-body-sm text-body-sm text-on-surface-variant">DNI {{ c.dni }} • {{ c.direccion }}@if (c.estadoCliente === 'BAJA') { • <strong class="text-error">BAJA</strong> }</span>
                    </div>
                  </div>
                </td>
                <td class="py-4 px-space-md text-right">
                  @if (c.enMora) {
                    <div class="flex flex-col items-end">
                      <span class="font-currency-md text-currency-md text-error font-bold tabular-nums">S/ {{ c.saldoAlCorte | soles }}</span>
                      <span class="text-[0.8125rem] text-error font-medium">+ S/ {{ c.moraAplicada | soles }} mora aplicada</span>
                    </div>
                  } @else {
                    <span class="font-currency-md text-currency-md text-on-surface font-bold tabular-nums">S/ {{ c.saldoAlCorte | soles }}</span>
                    <span class="block text-[0.8125rem] text-on-surface-variant">Corte: {{ c.fechaCorte | fecha: true }} · Pago: {{ c.fechaPago | fecha: true }}</span>
                  }
                </td>
                <td class="py-4 px-space-md text-right">
                  <span class="font-body-md text-body-md text-on-surface-variant font-semibold tabular-nums">S/ {{ c.creditoLibre | soles }}</span>
                  <div class="w-24 ml-auto mt-1 h-1.5 bg-surface-container-high rounded-full overflow-hidden">
                    <div class="h-full bg-secondary-container" [style.width.%]="c.usoPorcentaje"></div>
                  </div>
                </td>
                <td class="py-4 px-space-md text-center">
                  @if (c.enMora) {
                    <div class="inline-flex items-center gap-1 rotate-[-1deg] bg-error text-on-error px-3 py-1 rounded-full font-label-badge text-label-badge uppercase shadow-sm">
                      <span class="material-symbols-outlined text-[0.9375rem]">timer</span>MORA ({{ c.diasMora }} {{ c.diasMora === 1 ? 'día' : 'días' }})
                    </div>
                  } @else if (c.idEstadoPendiente) {
                    <div class="inline-flex items-center gap-1 rotate-[1deg] bg-tertiary-fixed text-on-tertiary-fixed px-3 py-1 rounded-full font-label-badge text-label-badge uppercase shadow-sm font-bold">
                      <span class="material-symbols-outlined text-[0.9375rem]">schedule</span>POR PAGAR
                    </div>
                  } @else {
                    <div class="inline-flex items-center gap-1 bg-[#00C853] text-[#111111] px-3 py-1 rounded-full font-label-badge text-label-badge uppercase shadow-sm font-bold" [class]="i % 2 ? 'rotate-[1deg]' : 'rotate-[-1deg]'">
                      <span class="material-symbols-outlined text-[0.9375rem]">check_circle</span>¡AL DÍA!
                    </div>
                  }
                </td>
                <td class="py-4 px-space-md text-right">
                  <div class="flex items-center justify-end gap-2">
                    @if (c.idEstadoPendiente) {
                      <a [routerLink]="['/pagos']" [queryParams]="{ cliente: c.id }" class="px-3 py-1.5 rounded-lg font-label-md text-label-md hover:opacity-90 shadow-sm flex items-center gap-1"
                         [class]="c.enMora ? 'bg-primary text-on-primary' : 'bg-primary-container text-on-primary-container'">
                        <span class="material-symbols-outlined text-[1rem]">receipt_long</span>Cobrar S/ {{ c.totalACobrar | soles }}
                      </a>
                    } @else {
                      <a [routerLink]="['/nueva-compra']" [queryParams]="{ cliente: c.id }" class="px-3 py-1.5 rounded-lg bg-surface-container-high hover:bg-surface-container-highest text-on-surface font-label-md text-label-md shadow-sm flex items-center gap-1">
                        <span class="material-symbols-outlined text-[1rem]">add_shopping_cart</span>Fiar
                      </a>
                    }
                    <a [routerLink]="['/clientes', c.id]" class="p-1.5 rounded-lg bg-surface-container hover:bg-surface-container-high text-on-surface" title="Ver ficha completa">
                      <span class="material-symbols-outlined text-[1.125rem]">visibility</span>
                    </a>
                    @if (c.idEstadoPendiente) {
                      <a class="p-1.5 rounded-lg bg-surface-container hover:bg-[#25D366]/20 text-[#25D366]" title="Enviar mensaje de cobro por WhatsApp" target="_blank" rel="noopener"
                         [href]="whatsapp(c)">
                        <span class="material-symbols-outlined text-[1.125rem]">chat</span>
                      </a>
                    }
                  </div>
                </td>
              </tr>
            } @empty {
              <tr><td colspan="5" class="py-10 text-center text-on-surface-variant">{{ vacio() }}</td></tr>
            }
          </tbody>
        </table>
      </div>
      <div class="p-space-md bg-surface-container-low flex flex-col md:flex-row items-center justify-between gap-space-sm text-on-surface-variant">
        <div class="flex items-center gap-2">
          <span class="material-symbols-outlined text-tertiary text-[1.125rem]">gavel</span>
          <span class="font-body-sm text-body-sm">{{ pie() }}</span>
        </div>
        <div class="flex items-center gap-space-sm">
          <span class="font-body-sm text-body-sm text-on-surface">Mostrando {{ visibles().length }} de {{ clientes().length }} clientes</span>
          <div class="flex items-center gap-1">
            <button type="button" class="w-8 h-8 rounded bg-surface-container hover:bg-surface-container-high flex items-center justify-center text-on-surface disabled:opacity-40" [disabled]="pagina() === 0" (click)="pagina.set(pagina() - 1)"><span class="material-symbols-outlined text-[1.125rem]">chevron_left</span></button>
            @for (p of numeros(); track p) {
              <button type="button" class="w-8 h-8 rounded flex items-center justify-center" [class]="p === pagina() ? 'bg-primary-container text-on-primary-container font-bold' : 'bg-surface-container hover:bg-surface-container-high text-on-surface'" (click)="pagina.set(p)">{{ p + 1 }}</button>
            }
            <button type="button" class="w-8 h-8 rounded bg-surface-container hover:bg-surface-container-high flex items-center justify-center text-on-surface disabled:opacity-40" [disabled]="pagina() >= numeros().length - 1" (click)="pagina.set(pagina() + 1)"><span class="material-symbols-outlined text-[1.125rem]">chevron_right</span></button>
          </div>
        </div>
      </div>
    </div>`,
})
export class TablaClientes {
  clientes = input.required<ClienteResumen[]>();
  pie = input<string>('');
  tienda = input<string>('la bodega');
  vacio = input<string>('Todavía no tienes clientes registrados.');
  porPagina = input<number>(8);
  cambio = output<void>();
  pagina = signal(0);
  numeros = computed(() => Array.from({ length: Math.max(1, Math.ceil(this.clientes().length / this.porPagina())) }, (_, i) => i));
  visibles = computed(() => {
    const p = Math.min(this.pagina(), this.numeros().length - 1);
    return this.clientes().slice(p * this.porPagina(), (p + 1) * this.porPagina());
  });

  whatsapp(c: ClienteResumen) {
    const nombre = c.nombres.split(' ')[0];
    const texto = `Hola ${nombre}, te saluda ${this.tienda()}. Te recuerdo con cariño tu estado de cuenta de S/ ${c.totalACobrar.toFixed(2)}`
      + (c.enMora ? ` (incluye S/ ${c.moraAplicada.toFixed(2)} de mora).` : ` con pago hasta el ${c.fechaPago.split('-').reverse().join('/')}.`)
      + ' Puedes pagarlo con Yape, Plin o en efectivo. ¡Gracias!';
    return `https://wa.me/51${c.telefono}?text=${encodeURIComponent(texto)}`;
  }
}

