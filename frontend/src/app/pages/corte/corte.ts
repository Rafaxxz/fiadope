import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { mensajeError } from '../../core/auth.interceptor';
import { CorteCliente, CortePreview } from '../../core/models';
import { AvisoService } from '../../shared/aviso';
import { AyudaModal } from '../../shared/ayuda-modal';
import { AYUDAS } from '../../shared/ayudas';
import { FechaPipe, hoyIso, InicialesPipe, MESES, SolesPipe } from '../../shared/formato';

/** P09 – Corte del mes (HU09). */
@Component({
  selector: 'fp-corte',
  standalone: true,
  imports: [FormsModule, RouterLink, SolesPipe, FechaPipe, InicialesPipe, AyudaModal],
  templateUrl: './corte.html',
})
export class CortePage implements OnInit {
  private api = inject(ApiService);
  private avisos = inject(AvisoService);
  auth = inject(AuthService);

  meses = MESES;
  ayudaCorte = AYUDAS['corte'];
  hoy = hoyIso();
  anio = signal(new Date().getFullYear());
  mes = signal(new Date().getMonth() + 1);
  corte = signal<CortePreview | null>(null);
  seleccion = signal<Set<number>>(new Set());
  q = signal('');
  detalle = signal<CorteCliente | null>(null);
  ayuda = signal(false);
  enviando = signal(false);
  cargando = signal(false);
  anios = Array.from({ length: 4 }, (_, i) => new Date().getFullYear() - 2 + i);

  filas = computed(() => {
    const q = this.q().toLowerCase().trim();
    return (this.corte()?.clientes ?? []).filter((c) => !q || c.nombres.toLowerCase().includes(q) || c.dni.includes(q));
  });
  generables = computed(() => this.filas().filter((c) => this.generable(c)));
  todos = computed(() => this.generables().length > 0 && this.generables().every((c) => this.seleccion().has(c.idCliente)));
  totalSeleccion = computed(() => this.filas().filter((c) => this.seleccion().has(c.idCliente)).reduce((s, c) => s + c.total, 0));
  generados = computed(() => (this.corte()?.clientes ?? []).filter((c) => c.generado).length);
  fechaPagoComun = computed(() => {
    const fechas = [...new Set((this.corte()?.clientes ?? []).map((c) => c.fechaPago))].sort();
    return fechas[0] ?? null;
  });
  fechaCorteComun = computed(() => [...new Set((this.corte()?.clientes ?? []).map((c) => c.fechaCorte))].sort()[0] ?? null);

  ngOnInit() { this.cargar(); }

  cargar() {
    this.cargando.set(true);
    this.api.corte(this.anio(), this.mes()).subscribe({
      next: (c) => {
        this.corte.set(c);
        this.cargando.set(false);
        this.seleccion.set(new Set(c.clientes.filter((x) => this.generable(x)).map((x) => x.idCliente)));
      },
      error: (e) => { this.cargando.set(false); this.avisos.error(mensajeError(e)); },
    });
  }

  generable(c: CorteCliente) { return !c.generado && c.items.length > 0 && c.fechaCorte <= this.hoy; }

  estado(c: CorteCliente): { texto: string; clase: string } {
    if (c.generado && c.estado === 'PAGADO') return { texto: 'PAGADO', clase: 'bg-[#00C853] text-[#111111]' };
    if (c.generado && c.fechaPago < this.hoy) return { texto: 'VENCIDO · EN MORA', clase: 'bg-error text-on-error' };
    if (c.generado) return { texto: c.enviado ? 'ENVIADO' : 'GENERADO', clase: 'bg-secondary-container text-on-secondary' };
    if (c.items.length === 0) return { texto: 'SIN CONSUMOS', clase: 'bg-surface-container-high text-on-surface-variant' };
    if (c.fechaCorte > this.hoy) return { texto: 'CORTE EL ' + c.fechaCorte.substring(8, 10) + '/' + c.fechaCorte.substring(5, 7), clase: 'bg-surface-container-high text-on-surface' };
    return { texto: 'LISTO PARA ENVÍO', clase: 'bg-tertiary-fixed text-on-tertiary-fixed' };
  }

  alternar(id: number) {
    const s = new Set(this.seleccion());
    s.has(id) ? s.delete(id) : s.add(id);
    this.seleccion.set(s);
  }

  alternarTodos() {
    this.seleccion.set(this.todos() ? new Set() : new Set(this.generables().map((c) => c.idCliente)));
  }

  cambiarMes(delta: number) {
    let m = this.mes() + delta;
    let a = this.anio();
    if (m < 1) { m = 12; a--; }
    if (m > 12) { m = 1; a++; }
    this.mes.set(m); this.anio.set(a); this.cargar();
  }

  generar() {
    const ids = [...this.seleccion()];
    if (!ids.length) return;
    this.enviando.set(true);
    this.api.generarCorte(this.anio(), this.mes(), ids).subscribe({
      next: (r) => {
        this.enviando.set(false);
        if (r.generados) this.avisos.ok(`¡Listo! ${r.generados} estado(s) de cuenta generado(s) y enviado(s) por correo.`);
        if (r.omitidos) this.avisos.info(r.detalle.filter((d) => !d.includes('enviado')).join(' · '));
        this.cargar();
      },
      error: (e) => { this.enviando.set(false); this.avisos.error(mensajeError(e)); },
    });
  }

  pdf(c: CorteCliente) {
    if (!c.idEstado) return;
    this.api.descargar(this.api.estadoPdf(c.idEstado), `estado-cuenta-${c.dni}-${c.fechaCorte}.pdf`, (ok) => { if (!ok) this.avisos.error('No se pudo descargar el PDF'); });
  }

  whatsapp(c: CorteCliente) {
    const lineas = c.items.map((i) => `${i.fecha.substring(8, 10)}/${i.fecha.substring(5, 7)} ${i.detalle}: S/ ${i.total.toFixed(2)}`).join('\n');
    const texto = `Hola ${c.nombres.split(' ')[0]}, tu estado de cuenta al ${c.fechaCorte.split('-').reverse().join('/')} en ${this.auth.perfil()?.tienda?.razonSocial}:\n${lineas}\nTotal: S/ ${c.total.toFixed(2)} hasta el ${c.fechaPago.split('-').reverse().join('/')}. ¡Gracias!`;
    window.open(`https://wa.me/?text=${encodeURIComponent(texto)}`, '_blank', 'noopener');
  }
}

