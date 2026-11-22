import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { mensajeError } from '../../core/auth.interceptor';
import { ClienteResumen } from '../../core/models';
import { AvisoService } from '../../shared/aviso';
import { SolesPipe } from '../../shared/formato';
import { TablaClientes } from '../../shared/tabla-clientes';

/** Lista de clientes (acceso a la ficha P06). */
@Component({
  selector: 'fp-clientes',
  standalone: true,
  imports: [FormsModule, RouterLink, TablaClientes, SolesPipe],
  template: `
    <div class="max-w-[88rem] mx-auto w-full px-gutter lg:px-gutter-desktop py-space-xl flex flex-col gap-space-lg">
      <div class="flex flex-col md:flex-row md:items-end justify-between gap-space-md">
        <div>
          <span class="font-label-badge text-label-badge px-2.5 py-1 rounded-lg bg-secondary-container text-on-secondary uppercase shadow-sm">Libretas del barrio</span>
          <h1 class="font-headline-lg text-headline-lg text-on-surface uppercase tracking-tight mt-2">Mis <span class="text-primary-container">clientes</span></h1>
          <p class="font-body-md text-body-md text-on-surface-variant">{{ clientes().length }} {{ clientes().length === 1 ? 'vecino' : 'vecinos' }} · límite asignado S/ {{ asignado() | soles }} de S/ {{ auth.perfil()?.tienda?.limiteGlobal | soles }}</p>
        </div>
        <div class="flex items-center gap-space-sm w-full md:w-auto">
          <div class="relative w-full md:w-80">
            <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant text-[1.25rem]">search</span>
            <input class="w-full pl-10 pr-4 py-3 bg-surface-container-lowest rounded-lg font-body-sm text-body-sm shadow-sm focus:outline-none focus:ring-2 focus:ring-primary/40" placeholder="Buscar por vecino o DNI..." [ngModel]="q()" (ngModelChange)="q.set($event)" />
          </div>
          <a routerLink="/clientes/nuevo" class="shrink-0 px-space-lg py-3 rounded-lg bg-primary-container text-on-primary font-headline-sm text-headline-sm uppercase shadow-md flex items-center gap-2 hover:brightness-110">
            <span class="material-symbols-outlined">person_add</span><span class="hidden sm:inline">Agregar cliente</span>
          </a>
        </div>
      </div>
      <fp-tabla-clientes [clientes]="filtrados()" [tienda]="auth.perfil()?.tienda?.razonSocial ?? ''" [porPagina]="10" [cargando]="cargando()"
                         pie="Haz clic en el nombre para ver la ficha, sus condiciones y su consentimiento." vacio="No hay clientes que coincidan." />
    </div>`,
})
export class ClientesPage implements OnInit {
  private api = inject(ApiService);
  private avisos = inject(AvisoService);
  auth = inject(AuthService);
  clientes = signal<ClienteResumen[]>([]);
  q = signal('');
  cargando = signal(true);
  filtrados = computed(() => {
    const q = this.q().toLowerCase().trim();
    return this.clientes().filter((c) => !q || c.nombres.toLowerCase().includes(q) || c.dni.includes(q));
  });
  asignado = computed(() => this.clientes().filter((c) => c.estadoCliente === 'ACTIVO').reduce((s, c) => s + Number(c.limite), 0));
  ngOnInit() { this.api.clientes().subscribe({ next: (c) => { this.clientes.set(c); this.cargando.set(false); }, error: (e) => { this.cargando.set(false); this.avisos.error(mensajeError(e)); } }); }
}
