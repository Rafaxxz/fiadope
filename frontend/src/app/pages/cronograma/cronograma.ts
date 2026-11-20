import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { mensajeError } from '../../core/auth.interceptor';
import { ClienteResumen, Compra, Cronograma } from '../../core/models';
import { AvisoService } from '../../shared/aviso';
import { AyudaModal } from '../../shared/ayuda-modal';
import { AYUDAS } from '../../shared/ayudas';
import { FechaPipe, SolesPipe } from '../../shared/formato';
import { InfoTip } from '../../shared/info-tip';

const DIAS = ['Domingo', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado'];

/** P08 – Cronograma de cuotas (HU08). También lo usa el cliente desde «Mi libreta». */
@Component({
  selector: 'fp-cronograma',
  standalone: true,
  imports: [FormsModule, RouterLink, SolesPipe, FechaPipe, InfoTip, AyudaModal],
  templateUrl: './cronograma.html',
})
export class CronogramaPage {
  private api = inject(ApiService);
  private avisos = inject(AvisoService);
  private router = inject(Router);

  id = input<string>();
  ayudaCrono = AYUDAS['cronograma'];
  crono = signal<Cronograma | null>(null);
  clientes = signal<ClienteResumen[]>([]);
  idCliente = signal<number | null>(null);
  compras = signal<Compra[]>([]);
  ayuda = signal(false);
  descargando = signal(false);

  enCuotas = computed(() => this.compras().filter((c) => c.modalidad === 'CUOTAS'));

  constructor() {
    effect(() => {
      const id = this.id();
      if (id) {
        this.api.cronograma(Number(id)).subscribe({
          next: (c) => { this.crono.set(c); this.idCliente.set(c.compra.idCliente); },
          error: (e) => { this.avisos.error(mensajeError(e)); this.router.navigate(['/cronograma']); },
        });
      } else {
        this.crono.set(null);
      }
    });
    this.api.clientes().subscribe((c) => this.clientes.set(c));
    effect(() => {
      const idc = this.idCliente();
      if (idc) this.api.comprasCliente(idc).subscribe((x) => this.compras.set(x));
    });
  }

  diaSemana(f: string) { return DIAS[new Date(f + 'T12:00:00').getDay()]; }

  pdf() {
    const c = this.crono();
    if (!c) return;
    this.descargando.set(true);
    this.api.descargar(this.api.cronogramaPdf(c.compra.id), `cronograma-${c.dni}-${c.compra.id}.pdf`, (ok) => {
      this.descargando.set(false);
      if (!ok) this.avisos.error('No se pudo descargar el PDF');
    });
  }

  whatsapp() {
    const c = this.crono();
    if (!c) return;
    const lineas = c.filas.map((f) => `Cuota ${f.numero}: ${f.vencimiento.split('-').reverse().join('/')} — S/ ${f.cuota.toFixed(2)}`).join('\n');
    const texto = `Hola ${c.cliente.split(' ')[0]}, este es el cronograma de tu compra (${c.compra.producto}):\n${lineas}\nTotal: S/ ${c.totalVentaPlazo.toFixed(2)}. ¡Gracias!`;
    window.open(`https://wa.me/?text=${encodeURIComponent(texto)}`, '_blank', 'noopener');
  }

  imprimir() { window.print(); }
}

