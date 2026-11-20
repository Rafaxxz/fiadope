import { Component, computed, inject, input, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Subject, debounceTime, switchMap, catchError, of } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { mensajeError } from '../../core/auth.interceptor';
import { ClienteResumen, CompraRequest, Modalidad, Producto, Simulacion } from '../../core/models';
import { AvisoService } from '../../shared/aviso';
import { AyudaModal } from '../../shared/ayuda-modal';
import { AYUDAS } from '../../shared/ayudas';
import { FechaPipe, InicialesPipe, SolesPipe } from '../../shared/formato';
import { InfoTip } from '../../shared/info-tip';
import { ProductoImg } from '../../shared/producto-img';

function ahoraLocal(): string {
  const d = new Date();
  d.setMinutes(d.getMinutes() - d.getTimezoneOffset());
  return d.toISOString().substring(0, 16);
}

/** P07 – Nueva compra (HU07): simulación antes de confirmar. */
@Component({
  selector: 'fp-compra',
  standalone: true,
  imports: [FormsModule, RouterLink, SolesPipe, FechaPipe, InicialesPipe, InfoTip, AyudaModal, ProductoImg],
  templateUrl: './compra.html',
})
export class CompraPage implements OnInit {
  private api = inject(ApiService);
  private avisos = inject(AvisoService);
  private router = inject(Router);

  cliente = input<string>();
  producto = input<string>();
  modalidadInicial = input<string>('', { alias: 'modalidad' });

  ayudaCompra = AYUDAS['compra'];
  clientes = signal<ClienteResumen[]>([]);
  productos = signal<Producto[]>([]);
  qCliente = signal('');
  qProducto = signal('');
  abiertoCliente = signal(false);
  abiertoProducto = signal(false);
  idCliente = signal<number | null>(null);
  idProducto = signal<number | null>(null);
  cantidad = signal<number>(1);
  fechaHora = signal<string>(ahoraLocal());
  modalidad = signal<Modalidad>('FIN_DE_MES');
  nCuotas = signal<number>(2);
  sim = signal<Simulacion | null>(null);
  errorSim = signal<string | null>(null);
  calculando = signal(false);
  guardando = signal(false);
  ayuda = signal(false);
  maxFecha = ahoraLocal();
  private cambios = new Subject<CompraRequest | null>();

  clienteSel = computed(() => this.clientes().find((c) => c.id === this.idCliente()) ?? null);
  productoSel = computed(() => this.productos().find((p) => p.id === this.idProducto()) ?? null);
  clientesFiltrados = computed(() => {
    const q = this.qCliente().toLowerCase().trim();
    return this.clientes().filter((c) => c.estadoCliente === 'ACTIVO' && (!q || c.nombres.toLowerCase().includes(q) || c.dni.includes(q))).slice(0, 8);
  });
  productosFiltrados = computed(() => {
    const q = this.qProducto().toLowerCase().trim();
    return this.productos().filter((p) => !q || p.nombre.toLowerCase().includes(q) || p.sku.toLowerCase().includes(q)).slice(0, 8);
  });
  maxCuotas = computed(() => this.sim()?.maxCuotas ?? 3);
  pastillas = computed(() => Array.from({ length: Math.max(4, Math.min(this.maxCuotas(), 12)) - 1 }, (_, i) => i + 2));
  capital = computed(() => (this.productoSel()?.precioLista ?? 0) * (this.cantidad() || 0));

  constructor() {
    this.cambios.pipe(
      debounceTime(250),
      switchMap((req) => {
        if (!req) return of(null);
        this.calculando.set(true);
        return this.api.simular(req).pipe(catchError((e) => { this.errorSim.set(mensajeError(e)); return of(null); }));
      }),
    ).subscribe((s) => {
      this.calculando.set(false);
      this.sim.set(s);
      if (s) this.errorSim.set(null);
    });
  }

  ngOnInit() {
    this.api.clientes().subscribe((c) => {
      this.clientes.set(c);
      const id = Number(this.cliente());
      if (id && c.some((x) => x.id === id)) this.elegirCliente(c.find((x) => x.id === id)!);
    });
    this.api.productos({ estado: 'ACTIVO' }).subscribe((p) => {
      this.productos.set(p);
      const id = Number(this.producto());
      const prod = p.find((x) => x.id === id);
      if (prod) this.elegirProducto(prod);
      if (this.modalidadInicial() === 'CUOTAS' || this.modalidadInicial() === 'FIN_DE_MES') this.modalidad.set(this.modalidadInicial() as Modalidad);
      this.ajustarModalidad();
      this.simular();
    });
  }

  elegirCliente(c: ClienteResumen) {
    this.idCliente.set(c.id);
    this.qCliente.set(`${c.nombres} (DNI ${c.dni})`);
    this.abiertoCliente.set(false);
    this.simular();
  }

  elegirProducto(p: Producto) {
    this.idProducto.set(p.id);
    this.qProducto.set(p.nombre);
    this.abiertoProducto.set(false);
    this.ajustarModalidad();
    this.simular();
  }

  ajustarModalidad() {
    const p = this.productoSel();
    if (!p) return;
    if (this.modalidad() === 'CUOTAS' && !p.permiteCuotas) this.modalidad.set('FIN_DE_MES');
    if (this.modalidad() === 'FIN_DE_MES' && !p.permiteFinMes) this.modalidad.set('CUOTAS');
  }

  setModalidad(m: Modalidad) {
    const p = this.productoSel();
    if (p && ((m === 'CUOTAS' && !p.permiteCuotas) || (m === 'FIN_DE_MES' && !p.permiteFinMes))) return;
    this.modalidad.set(m);
    this.simular();
  }

  setCuotas(n: number) {
    if (n > this.maxCuotas()) return;
    this.nCuotas.set(n);
    this.simular();
  }

  peticion(): CompraRequest | null {
    if (!this.idCliente() || !this.idProducto() || !(this.cantidad() > 0)) return null;
    return {
      idCliente: this.idCliente()!, idProducto: this.idProducto()!, cantidad: this.cantidad(),
      fechaHora: this.fechaHora() ? this.fechaHora() + ':00' : null, modalidad: this.modalidad(),
      nCuotas: this.modalidad() === 'CUOTAS' ? this.nCuotas() : null,
    };
  }

  simular() {
    const req = this.peticion();
    if (!req) { this.sim.set(null); }
    this.cambios.next(req);
  }

  confirmar() {
    const req = this.peticion();
    const s = this.sim();
    if (!req || !s || s.excedeLimite) return;
    this.guardando.set(true);
    this.api.registrarCompra(req).subscribe({
      next: (c) => {
        this.guardando.set(false);
        this.avisos.ok(`¡Compra fiada registrada! ${c.cliente} paga ${c.modalidad === 'CUOTAS' ? c.nCuotas + ' cuotas de S/ ' + c.cuotaFija?.toFixed(2) : 'S/ ' + c.montoS.toFixed(2) + ' el ' + c.fechaPagoAsignada.split('-').reverse().join('/')}.`);
        this.router.navigate(c.modalidad === 'CUOTAS' ? ['/cronograma', c.id] : ['/clientes', c.idCliente]);
      },
      error: (e) => { this.guardando.set(false); this.avisos.error(mensajeError(e)); this.simular(); },
    });
  }

  cerrarListas() { setTimeout(() => { this.abiertoCliente.set(false); this.abiertoProducto.set(false); }, 150); }
}

