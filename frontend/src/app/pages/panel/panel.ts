import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { mensajeError } from '../../core/auth.interceptor';
import { Panel } from '../../core/models';
import { AvisoService } from '../../shared/aviso';
import { AyudaModal } from '../../shared/ayuda-modal';
import { AYUDAS } from '../../shared/ayudas';
import { FechaPipe, SolesPipe } from '../../shared/formato';
import { TablaClientes } from '../../shared/tabla-clientes';
import { Contador } from '../../shared/contador';
import { Esqueleto } from '../../shared/esqueleto';

/** P04 – Panel del negocio (HU04). */
@Component({
  selector: 'fp-panel',
  standalone: true,
  imports: [FormsModule, RouterLink, SolesPipe, FechaPipe, TablaClientes, AyudaModal, Contador, Esqueleto],
  templateUrl: './panel.html',
})
export class PanelPage implements OnInit {
  private api = inject(ApiService);
  private avisos = inject(AvisoService);
  auth = inject(AuthService);

  ayudaPanel = AYUDAS['panel'];
  panel = signal<Panel | null>(null);
  buscar = signal('');
  filtro = signal<'todos' | 'mora' | 'aldia'>('todos');
  ayuda = signal(false);
  ahora = new Date();

  saludo = computed(() => {
    const h = this.ahora.getHours();
    return h < 12 ? 'Buenos días' : h < 19 ? 'Buenas tardes' : 'Buenas noches';
  });
  turno = computed(() => {
    const h = this.ahora.getHours();
    return h < 12 ? 'Turno Mañana' : h < 19 ? 'Turno Tarde' : 'Turno Noche';
  });
  nombre = computed(() => (this.panel()?.propietario ?? this.auth.perfil()?.nombre ?? '').split(' ')[0]);
  clientes = computed(() => {
    const q = this.buscar().toLowerCase().trim();
    const f = this.filtro();
    return (this.panel()?.clientes ?? []).filter((c) =>
      (!q || c.nombres.toLowerCase().includes(q) || c.dni.includes(q)) &&
      (f === 'todos' || (f === 'mora' ? c.enMora : !c.enMora)));
  });
  usoLimite = computed(() => { const p = this.panel(); return p && p.limiteGlobal ? Math.min(100, (p.totalPorCobrar / p.limiteGlobal) * 100) : 0; });
  pctMora = computed(() => { const p = this.panel(); return p && p.totalPorCobrar ? Math.min(100, (p.montoEnMora / p.totalPorCobrar) * 100) : 0; });
  maxTop = computed(() => Math.max(0, ...(this.panel()?.topProductos ?? []).map((t) => t.monto)));
  primerMoroso = computed(() => this.panel()?.clientes.find((c) => c.idEstadoPendiente) ?? null);

  ngOnInit() {
    this.api.panel().subscribe({ next: (p) => this.panel.set(p), error: (e) => this.avisos.error(mensajeError(e)) });
  }

  siguienteFiltro() {
    const orden: ('todos' | 'mora' | 'aldia')[] = ['todos', 'mora', 'aldia'];
    this.filtro.set(orden[(orden.indexOf(this.filtro()) + 1) % 3]);
  }
}

