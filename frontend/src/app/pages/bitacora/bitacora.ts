import { Component, computed, inject, input, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { mensajeError } from '../../core/auth.interceptor';
import { Pagina, RegistroBitacora } from '../../core/models';
import { AvisoService } from '../../shared/aviso';
import { AyudaModal } from '../../shared/ayuda-modal';
import { AYUDAS } from '../../shared/ayudas';
import { FechaPipe } from '../../shared/formato';
import { InfoTip } from '../../shared/info-tip';

const NOMBRES: Record<string, { texto: string; color: string }> = {
  LOGIN: { texto: 'Inicio de sesión', color: 'bg-secondary' },
  LOGOUT: { texto: 'Cierre de sesión', color: 'bg-outline' },
  LOGIN_FALLIDO: { texto: 'Contraseña incorrecta', color: 'bg-error' },
  LOGIN_RECHAZADO: { texto: 'Acceso rechazado', color: 'bg-error' },
  BLOQUEO_CUENTA: { texto: 'Cuenta bloqueada 15 min', color: 'bg-error' },
  MFA_FALLIDO: { texto: 'Código MFA incorrecto', color: 'bg-error' },
  MFA_REENVIO: { texto: 'Reenvío de código MFA', color: 'bg-tertiary' },
  ACCESO_DENEGADO: { texto: 'Intento de ver datos ajenos', color: 'bg-error' },
  COMPRA: { texto: 'Nueva compra fiada', color: 'bg-primary' },
  PAGO: { texto: 'Registro de pago total', color: 'bg-secondary' },
  CORTE: { texto: 'Estado de cuenta generado', color: 'bg-tertiary' },
  ALTA_CLIENTE: { texto: 'Alta de cliente', color: 'bg-primary-container' },
  EDITA_CLIENTE: { texto: 'Edición de cliente', color: 'bg-secondary-container' },
  CAMBIO_TASA: { texto: 'Cambio de tasa', color: 'bg-secondary-container' },
  BAJA_CLIENTE: { texto: 'Baja de cliente', color: 'bg-error' },
  REACTIVA_CLIENTE: { texto: 'Reactivación de cliente', color: 'bg-secondary' },
  CONSENTIMIENTO: { texto: 'Consentimiento Ley 29733', color: 'bg-tertiary' },
  ALTA_PRODUCTO: { texto: 'Alta de producto', color: 'bg-primary-container' },
  EDITA_PRODUCTO: { texto: 'Edición de producto', color: 'bg-secondary-container' },
  BAJA_PRODUCTO: { texto: 'Baja de producto', color: 'bg-error' },
  REACTIVA_PRODUCTO: { texto: 'Reactivación de producto', color: 'bg-secondary' },
  ALTA_TIENDA: { texto: 'Alta de tienda', color: 'bg-primary-container' },
  EDITA_TIENDA: { texto: 'Edición de tienda', color: 'bg-secondary-container' },
  BAJA_TIENDA: { texto: 'Baja de tienda', color: 'bg-error' },
  REACTIVA_TIENDA: { texto: 'Reactivación de tienda', color: 'bg-secondary' },
  CAMBIO_TOPES: { texto: 'Actualización de topes BCRP', color: 'bg-tertiary' },
};
const ALERTAS = ['LOGIN_FALLIDO', 'LOGIN_RECHAZADO', 'BLOQUEO_CUENTA', 'MFA_FALLIDO', 'ACCESO_DENEGADO'];

/** P12 – Bitácora (HU12): solo lectura, con filtros. */
import { Esqueleto } from '../../shared/esqueleto';
@Component({
  selector: 'fp-bitacora',
  standalone: true,
  imports: [FormsModule, FechaPipe, InfoTip, AyudaModal, Esqueleto],
  templateUrl: './bitacora.html',
})
export class BitacoraPage implements OnInit {
  private api = inject(ApiService);
  private avisos = inject(AvisoService);
  auth = inject(AuthService);

  idTienda = input<string>();
  tienda = input<string>();
  usuarioInicial = input<string>('', { alias: 'usuario' });

  ayudaBitacora = AYUDAS['bitacora'];
  pagina = signal<Pagina<RegistroBitacora> | null>(null);
  acciones = signal<string[]>([]);
  usuario = '';
  accion = '';
  ip = '';
  desde = '';
  hasta = '';
  numero = signal(0);
  ayuda = signal(false);
  cargando = signal(false);
  abierto = signal<number | null>(null);
  errorFiltro = signal<string | null>(null);

  alertas = computed(() => (this.pagina()?.contenido ?? []).filter((r) => ALERTAS.includes(r.accion)).length);
  ultimoPago = computed(() => (this.pagina()?.contenido ?? []).find((r) => r.accion === 'PAGO') ?? null);

  ngOnInit() {
    this.usuario = this.usuarioInicial() ?? '';
    this.api.accionesBitacora().subscribe((a) => this.acciones.set(a));
    this.buscar();
  }

  buscar(n = 0) {
    if (this.desde && this.hasta && this.hasta < this.desde) { this.errorFiltro.set('El rango de fechas no es válido: la fecha final es anterior a la inicial'); return; }
    this.errorFiltro.set(null);
    this.numero.set(n);
    this.cargando.set(true);
    this.api.bitacora({ usuario: this.usuario, accion: this.accion, ip: this.ip, desde: this.desde, hasta: this.hasta, idTienda: this.idTienda(), pagina: n, tamano: 15 }).subscribe({
      next: (p) => { this.pagina.set(p); this.cargando.set(false); },
      error: (e) => { this.cargando.set(false); this.avisos.error(mensajeError(e)); },
    });
  }

  limpiar() { this.usuario = ''; this.accion = ''; this.ip = ''; this.desde = ''; this.hasta = ''; this.buscar(); }

  nombre(a: string) { return NOMBRES[a]?.texto ?? a; }
  color(a: string) { return NOMBRES[a]?.color ?? 'bg-outline'; }
  alerta(a: string) { return ALERTAS.includes(a); }

  valor(json: string | null, clave: string) { return this.campos(json).find((c) => c.k === clave)?.v ?? '—'; }

  /** Resume un JSON de la bitácora como «clave: valor». */
  campos(json: string | null): { k: string; v: string }[] {
    if (!json) return [];
    try {
      const o = JSON.parse(json);
      return Object.entries(o).map(([k, v]) => ({ k: k.replace(/_/g, ' '), v: v === null ? '—' : typeof v === 'object' ? JSON.stringify(v) : String(v) }));
    } catch { return [{ k: 'valor', v: json }]; }
  }
}

