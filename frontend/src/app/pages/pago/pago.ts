import { Component, computed, inject, input, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { erroresCampos, mensajeError } from '../../core/auth.interceptor';
import { ClienteResumen, Pago, PagoPreview } from '../../core/models';
import { AvisoService } from '../../shared/aviso';
import { AyudaModal } from '../../shared/ayuda-modal';
import { AYUDAS } from '../../shared/ayudas';
import { FechaPipe, hoyIso, InicialesPipe, SolesPipe } from '../../shared/formato';
import { InfoTip } from '../../shared/info-tip';

/** P10 – Registrar pago (HU10): solo pago total, con intereses por mora. */
@Component({
  selector: 'fp-pago',
  standalone: true,
  imports: [FormsModule, RouterLink, SolesPipe, FechaPipe, InicialesPipe, InfoTip, AyudaModal],
  templateUrl: './pago.html',
})
export class PagoPage implements OnInit {
  private api = inject(ApiService);
  private avisos = inject(AvisoService);

  cliente = input<string>();
  ayudaPago = AYUDAS['pago'];
  metodos = [
    { valor: 'Yape/Plin', titulo: 'Yape / Plin', detalle: 'Billetera digital instantánea', emoji: '📱' },
    { valor: 'Efectivo', titulo: 'Efectivo en mostrador', detalle: 'Billetes y monedas recibidos', emoji: '💵' },
    { valor: 'Transferencia', titulo: 'Transferencia bancaria', detalle: 'BCP, Interbank, BBVA o Banco de la Nación', emoji: '🏦' },
  ];
  hoy = hoyIso();
  pendientes = signal<ClienteResumen[]>([]);
  idCliente = signal<number | null>(null);
  fecha = signal(hoyIso());
  preview = signal<PagoPreview | null>(null);
  error = signal<string | null>(null);
  metodo = signal('Yape/Plin');
  nroOperacion = signal('');
  monto = signal<number | null>(null);
  errores = signal<Record<string, string>>({});
  guardando = signal(false);
  pago = signal<Pago | null>(null);
  ayuda = signal(false);

  diferencia = computed(() => {
    const p = this.preview();
    const m = this.monto();
    return p && m !== null ? +(m - p.total).toFixed(2) : 0;
  });

  ngOnInit() {
    this.api.clientes().subscribe((c) => {
      this.pendientes.set(c.filter((x) => x.idEstadoPendiente));
      const id = Number(this.cliente());
      if (id) this.elegir(id);
    });
  }

  elegir(id: number | null) {
    this.idCliente.set(id);
    this.pago.set(null);
    this.cargar();
  }

  cargar() {
    const id = this.idCliente();
    this.preview.set(null);
    this.error.set(null);
    if (!id) return;
    this.api.previewPago({ idCliente: id, fecha: this.fecha() }).subscribe({
      next: (p) => { this.preview.set(p); this.monto.set(p.total); this.errores.set({}); },
      error: (e) => this.error.set(mensajeError(e)),
    });
  }

  confirmar() {
    const p = this.preview();
    if (!p) return;
    const e: Record<string, string> = {};
    if (this.metodo() !== 'Efectivo' && !this.nroOperacion().trim()) e['nroOperacion'] = 'El número de operación es obligatorio';
    if (this.monto() === null || this.diferencia() !== 0) e['montoPagado'] = `Solo se acepta el pago total de S/ ${p.total.toFixed(2)}. No se aceptan pagos parciales.`;
    this.errores.set(e);
    if (Object.keys(e).length) return;
    this.guardando.set(true);
    this.api.pagar({ idEstado: p.idEstado, fechaPagoReal: this.fecha(), metodoPago: this.metodo(), nroOperacion: this.metodo() === 'Efectivo' ? null : this.nroOperacion().trim(), montoPagado: this.monto()! }).subscribe({
      next: (r) => {
        this.guardando.set(false);
        this.pago.set(r);
        this.avisos.ok(`Cobro de S/ ${r.totalPagado.toFixed(2)} registrado. Libreta del período en S/ 0.00.`);
        this.api.clientes().subscribe((c) => this.pendientes.set(c.filter((x) => x.idEstadoPendiente)));
      },
      error: (err) => { this.guardando.set(false); this.errores.set(erroresCampos(err)); this.avisos.error(mensajeError(err)); },
    });
  }

  otro() { this.pago.set(null); this.cargar(); }

  constancia() {
    const p = this.preview(); const r = this.pago();
    if (!p || !r) return;
    const texto = `Hola ${p.cliente.split(' ')[0]}, registramos tu pago de S/ ${r.totalPagado.toFixed(2)} (${r.metodoPago}${r.nroOperacion ? ' op. ' + r.nroOperacion : ''}) del corte ${p.fechaCorte.split('-').reverse().join('/')}`
      + (r.diasMora ? `, que incluye S/ ${(r.intCompMora + r.intMora).toFixed(2)} de intereses por ${r.diasMora} días de mora` : '') + '. Tu libreta del período quedó en S/ 0.00. ¡Gracias!';
    window.open(`https://wa.me/?text=${encodeURIComponent(texto)}`, '_blank', 'noopener');
  }
}

