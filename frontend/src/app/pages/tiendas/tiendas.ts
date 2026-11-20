import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { erroresCampos, mensajeError } from '../../core/auth.interceptor';
import { Tienda, Topes } from '../../core/models';
import { AvisoService } from '../../shared/aviso';
import { AyudaModal } from '../../shared/ayuda-modal';
import { AYUDAS } from '../../shared/ayudas';
import { SolesPipe } from '../../shared/formato';
import { InfoTip } from '../../shared/info-tip';

export const GIROS = ['Bodega y abarrotes', 'Panadería', 'Pollería', 'Peluquería', 'Spa', 'Otro'];
const GIRO_ESTILO: Record<string, { emoji: string; icono: string; clase: string }> = {
  'Bodega y abarrotes': { emoji: '🏪', icono: 'local_convenience_store', clase: 'bg-secondary-fixed text-on-secondary-fixed' },
  'Panadería': { emoji: '🥖', icono: 'bakery_dining', clase: 'bg-tertiary-fixed text-on-tertiary-fixed' },
  'Pollería': { emoji: '🍗', icono: 'restaurant', clase: 'bg-secondary-fixed-dim text-on-secondary-fixed-variant' },
  'Peluquería': { emoji: '💇', icono: 'content_cut', clase: 'bg-surface-container-highest text-on-surface-variant' },
  'Spa': { emoji: '💆', icono: 'spa', clase: 'bg-primary-fixed text-on-primary-fixed-variant' },
  'Otro': { emoji: '🛍️', icono: 'storefront', clase: 'bg-surface-container-high text-on-surface' },
};

interface FormTienda { ruc: string; razonSocial: string; propietario: string; giro: string; ubicacion: string; direccion: string; telefono: string; limiteGlobal: number | null; adminEmail: string; adminClave: string; }

/** P03 – Gestión de tiendas (HU03). */
@Component({
  selector: 'fp-tiendas',
  standalone: true,
  imports: [FormsModule, SolesPipe, InfoTip, AyudaModal],
  templateUrl: './tiendas.html',
})
export class TiendasPage implements OnInit {
  private api = inject(ApiService);
  private avisos = inject(AvisoService);
  private router = inject(Router);

  giros = GIROS;
  ayudaTiendas = AYUDAS['tiendas'];
  tiendas = signal<Tienda[]>([]);
  cargando = signal(true);
  q = '';
  giro = '';
  estado = '';
  pagina = signal(0);
  porPagina = 8;
  ayuda = signal(false);
  modal = signal<'nueva' | 'editar' | null>(null);
  editando = signal<Tienda | null>(null);
  confirmarBaja = signal<Tienda | null>(null);
  form: FormTienda = this.vacio();
  errores = signal<Record<string, string>>({});
  errorForm = signal<string | null>(null);
  guardando = signal(false);
  topes = signal<Topes | null>(null);
  editTopes = signal(false);
  topeComp = 0;
  topeMor = 0;

  paginas = computed(() => Math.max(1, Math.ceil(this.tiendas().length / this.porPagina)));
  visibles = computed(() => this.tiendas().slice(this.pagina() * this.porPagina, (this.pagina() + 1) * this.porPagina));
  kpi = computed(() => {
    const t = this.tiendas();
    const activas = t.filter((x) => x.estado === 'ACTIVA');
    const limite = activas.reduce((s, x) => s + Number(x.limiteGlobal), 0);
    const asignado = activas.reduce((s, x) => s + Number(x.limiteAsignado), 0);
    return {
      limite, activas: activas.length, pctActivas: t.length ? (activas.length * 100) / t.length : 0,
      clientes: t.reduce((s, x) => s + x.clientes, 0), pctAsignado: limite ? (asignado * 100) / limite : 0,
    };
  });

  ngOnInit() {
    this.cargar();
    this.api.topesAdmin().subscribe((t) => this.topes.set(t));
  }

  estilo(giro: string) { return GIRO_ESTILO[giro] ?? GIRO_ESTILO['Otro']; }
  usado(t: Tienda) { return t.limiteGlobal ? Math.round((Number(t.limiteAsignado) * 100) / Number(t.limiteGlobal)) : 0; }

  cargar() {
    this.cargando.set(true);
    this.api.tiendas({ q: this.q, giro: this.giro, estado: this.estado }).subscribe({
      next: (t) => { this.tiendas.set(t); this.pagina.set(0); this.cargando.set(false); },
      error: (e) => { this.cargando.set(false); this.avisos.error(mensajeError(e)); },
    });
  }

  limpiar() { this.q = ''; this.giro = ''; this.estado = ''; this.cargar(); }

  vacio(): FormTienda {
    return { ruc: '', razonSocial: '', propietario: '', giro: 'Bodega y abarrotes', ubicacion: '', direccion: '', telefono: '', limiteGlobal: null, adminEmail: '', adminClave: '' };
  }

  nueva() { this.form = this.vacio(); this.errores.set({}); this.errorForm.set(null); this.editando.set(null); this.modal.set('nueva'); }

  editar(t: Tienda) {
    this.form = { ruc: t.ruc, razonSocial: t.razonSocial, propietario: t.propietario, giro: t.giro, ubicacion: t.ubicacion, direccion: t.direccion, telefono: t.telefono, limiteGlobal: t.limiteGlobal, adminEmail: 'sin-cambio@fiadope.pe', adminClave: '' };
    this.errores.set({}); this.errorForm.set(null); this.editando.set(t); this.modal.set('editar');
  }

  guardar() {
    const f = this.form;
    const e: Record<string, string> = {};
    if (!/^(10|20)\d{9}$/.test(f.ruc)) e['ruc'] = 'El RUC tiene 11 dígitos y empieza con 10 o 20';
    if (!f.razonSocial.trim()) e['razonSocial'] = 'Ingresa el nombre de la tienda';
    if (!f.propietario.trim()) e['propietario'] = 'Ingresa el propietario';
    if (!f.ubicacion.trim()) e['ubicacion'] = 'Ingresa el distrito';
    if (!f.direccion.trim()) e['direccion'] = 'Ingresa la dirección';
    if (!/^9\d{8}$/.test(f.telefono)) e['telefono'] = 'El teléfono tiene 9 dígitos y empieza con 9';
    if (!f.limiteGlobal || f.limiteGlobal <= 0) e['limiteGlobal'] = 'El límite debe ser mayor a 0';
    if (this.modal() === 'nueva') {
      if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(f.adminEmail)) e['adminEmail'] = 'Correo inválido';
      if (!/^(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,64}$/.test(f.adminClave)) e['adminClave'] = '8+ caracteres con mayúscula, número y símbolo';
    }
    this.errores.set(e);
    if (Object.keys(e).length) return;
    this.guardando.set(true);
    const t = this.editando();
    const peticion = t ? this.api.editarTienda(t.id, f) : this.api.crearTienda(f);
    peticion.subscribe({
      next: (r) => {
        this.guardando.set(false);
        this.modal.set(null);
        this.avisos.ok(t ? `Tienda ${r.razonSocial} actualizada` : `Tienda ${r.razonSocial} registrada como ACTIVA. Su usuario es el RUC ${r.ruc}.`);
        this.cargar();
      },
      error: (err) => { this.guardando.set(false); this.errores.set(erroresCampos(err)); this.errorForm.set(mensajeError(err)); },
    });
  }

  darDeBaja(t: Tienda) {
    this.api.bajaTienda(t.id).subscribe({
      next: () => { this.confirmarBaja.set(null); this.avisos.ok(`${t.razonSocial} quedó dada de baja: sus usuarios ya no pueden ingresar.`); this.cargar(); },
      error: (e) => this.avisos.error(mensajeError(e)),
    });
  }

  reactivar(t: Tienda) {
    this.api.reactivarTienda(t.id).subscribe({
      next: () => { this.avisos.ok(`${t.razonSocial} fue reactivada`); this.cargar(); },
      error: (e) => this.avisos.error(mensajeError(e)),
    });
  }

  verBitacora(t: Tienda) { this.router.navigate(['/bitacora'], { queryParams: { idTienda: t.id, tienda: t.razonSocial } }); }

  abrirTopes() {
    const t = this.topes();
    this.topeComp = t ? +(t.teaCompensatoria * 100).toFixed(2) : 0;
    this.topeMor = t ? +(t.teaMoratoria * 100).toFixed(2) : 0;
    this.editTopes.set(true);
  }

  guardarTopes() {
    this.api.guardarTopes({ teaCompensatoria: this.topeComp / 100, teaMoratoria: this.topeMor / 100 }).subscribe({
      next: (t) => { this.topes.set(t); this.editTopes.set(false); this.avisos.ok('Topes del BCRP actualizados'); },
      error: (e) => this.avisos.error(mensajeError(e)),
    });
  }

  exportar() {
    const filas = [['RUC', 'Tienda', 'Propietario', 'Giro', 'Ubicación', 'Límite global', 'Límite asignado', 'Clientes', 'Estado']]
      .concat(this.tiendas().map((t) => [t.ruc, t.razonSocial, t.propietario, t.giro, t.ubicacion, String(t.limiteGlobal), String(t.limiteAsignado), String(t.clientes), t.estado]));
    const csv = '﻿' + filas.map((f) => f.map((c) => `"${c.replace(/"/g, '""')}"`).join(';')).join('\n');
    const a = document.createElement('a');
    a.href = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }));
    a.download = 'tiendas-fiadope.csv';
    a.click();
  }
}

