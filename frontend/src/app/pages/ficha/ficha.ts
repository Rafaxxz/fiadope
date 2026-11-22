import { Component, computed, inject, input, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { erroresCampos, mensajeError } from '../../core/auth.interceptor';
import { ClienteFicha, ClienteRequest, Topes } from '../../core/models';
import { AvisoService } from '../../shared/aviso';
import { AyudaModal } from '../../shared/ayuda-modal';
import { AYUDAS } from '../../shared/ayudas';
import { FechaPipe, HoraPipe, InicialesPipe, SolesPipe } from '../../shared/formato';
import { InfoTip } from '../../shared/info-tip';

export const PLAZOS = [
  { dias: 360, texto: 'Anual', sigla: 'A' }, { dias: 180, texto: 'Semestral', sigla: 'S' }, { dias: 120, texto: 'Cuatrimestral', sigla: 'C' },
  { dias: 90, texto: 'Trimestral', sigla: 'T' }, { dias: 60, texto: 'Bimestral', sigla: 'B' }, { dias: 30, texto: 'Mensual', sigla: 'M' },
  { dias: 15, texto: 'Quincenal', sigla: 'Q' }, { dias: 1, texto: 'Diaria', sigla: 'D' },
];
export function nombrePlazo(d: number | null | undefined) { return PLAZOS.find((p) => p.dias === d)?.texto ?? `${d} días`; }
export function siglaTasa(tipo: string, dias: number) { return (tipo === 'NOMINAL' ? 'TN' : 'TE') + (PLAZOS.find((p) => p.dias === dias)?.sigla ?? ''); }

/** Equivalente TEA de una tasa (mismo cálculo que el motor del backend, solo para la vista previa). */
function teaEquivalente(tipo: string, tasaPct: number, plazo: number, cap: number | null): number {
  const t = tasaPct / 100;
  if (!t || !plazo) return 0;
  let ted: number;
  if (tipo === 'NOMINAL') {
    const c = cap || 1;
    ted = Math.pow(1 + t / (plazo / c), 1 / c) - 1;
  } else {
    ted = Math.pow(1 + t, 1 / plazo) - 1;
  }
  return (Math.pow(1 + ted, 360) - 1) * 100;
}

/** P06 – Ficha de cliente (HU06): ver, registrar y editar condiciones. */
import { Esqueleto } from '../../shared/esqueleto';
import { ValidacionViva } from '../../shared/validacion-viva';
@Component({
  selector: 'fp-ficha',
  standalone: true,
  imports: [FormsModule, RouterLink, SolesPipe, FechaPipe, HoraPipe, InicialesPipe, InfoTip, AyudaModal, Esqueleto],
  templateUrl: './ficha.html',
})
export class FichaPage implements OnInit {
  private api = inject(ApiService);
  private avisos = inject(AvisoService);
  private router = inject(Router);
  auth = inject(AuthService);

  id = input<string>();
  plazos = PLAZOS;
  nombrePlazo = nombrePlazo;
  ayudaClientes = AYUDAS['clientes'];
  ficha = signal<ClienteFicha | null>(null);
  modo = signal<'ver' | 'editar' | 'nuevo'>('ver');
  topes = signal<Topes | null>(null);
  form: ClienteRequest = this.vacio();
  errores = signal<Record<string, string>>({});
  vivo = new ValidacionViva(() => this.validar(), this.errores, { limite: 'limiteCredito', tasa: 'tasaPorcentaje', tasaMora: 'tasaMoraPorcentaje', acepta: 'aceptaPolitica' });
  errorForm = signal<string | null>(null);
  guardando = signal(false);
  ayuda = signal(false);
  confirmaBaja = signal(false);
  version = signal(0);

  teaPrev = computed(() => { this.version(); return teaEquivalente(this.form.tipoTasa, this.form.tasaPorcentaje, this.form.diasPlazoTasa, this.form.diasCapitalizacion); });
  teaMorPrev = computed(() => { this.version(); return teaEquivalente(this.form.tipoTasaMora, this.form.tasaMoraPorcentaje, this.form.diasPlazoTasa, this.form.diasCapitalizacion); });
  nivelUso = computed(() => {
    const u = this.ficha()?.resumen.usoPorcentaje ?? 0;
    return u < 70 ? { texto: 'Nivel seguro: menos del 70% de cupo ocupado', clase: 'text-secondary' }
      : u < 90 ? { texto: 'Atención: más del 70% del cupo ocupado', clase: 'text-tertiary' }
      : { texto: 'Cupo casi agotado', clase: 'text-error' };
  });
  antiguedad = computed(() => {
    const f = this.ficha()?.fechaRegistro;
    if (!f) return '';
    const meses = Math.max(0, Math.floor((Date.now() - new Date(f).getTime()) / (30 * 86400000)));
    return meses < 1 ? 'nuevo este mes' : meses < 12 ? `${meses} ${meses === 1 ? 'mes' : 'meses'}` : `${Math.floor(meses / 12)} año(s) y ${meses % 12} meses`;
  });
  siglaTasa = siglaTasa;

  ngOnInit() {
    this.api.topes().subscribe((t) => this.topes.set(t));
    if (!this.id() || this.id() === 'nuevo') {
      this.modo.set('nuevo');
      this.form = this.vacio();
    } else {
      this.cargar();
    }
  }

  cargar() {
    this.api.cliente(Number(this.id())).subscribe({
      next: (f) => { this.ficha.set(f); this.modo.set('ver'); },
      error: (e) => { this.avisos.error(mensajeError(e)); this.router.navigate(['/clientes']); },
    });
  }

  vacio(): ClienteRequest {
    return { dni: '', nombres: '', direccion: '', telefono: '', email: '', clave: '', tipoTasa: 'EFECTIVA', diasPlazoTasa: 360, diasCapitalizacion: 1, tasaPorcentaje: 24, tipoTasaMora: 'EFECTIVA', tasaMoraPorcentaje: 15, moneda: 'PEN', limiteCredito: 500, maxMeses: 3, diaCorte: 20, diaPago: 26, aceptaPolitica: false };
  }

  editar() {
    const f = this.ficha()!;
    this.form = { dni: f.dni, nombres: f.nombres, direccion: f.direccion, telefono: f.telefono, email: f.email, clave: '', tipoTasa: f.tipoTasa, diasPlazoTasa: f.diasPlazoTasa, diasCapitalizacion: f.diasCapitalizacion ?? 1, tasaPorcentaje: f.tasaPorcentaje, tipoTasaMora: f.tipoTasaMora, tasaMoraPorcentaje: f.tasaMoraPorcentaje, moneda: f.moneda, limiteCredito: f.limiteCredito, maxMeses: f.maxMeses, diaCorte: f.diaCorte, diaPago: f.diaPago, aceptaPolitica: true };
    this.vivo.reiniciar(); this.errores.set({}); this.errorForm.set(null); this.version.update((v) => v + 1);
    this.modo.set('editar');
  }

  cambio() { this.version.update((v) => v + 1); }

  validar(): Record<string, string> {
    const f = this.form;
    const e: Record<string, string> = {};
    const nuevo = this.modo() === 'nuevo';
    if (!/^\d{8,12}$/.test(f.dni)) e['dni'] = 'DNI de 8 dígitos o CE de 9 a 12';
    if (!f.nombres.trim()) e['nombres'] = 'Ingresa los nombres';
    if (!f.direccion.trim()) e['direccion'] = 'Ingresa la dirección';
    if (!/^9\d{8}$/.test(f.telefono)) e['telefono'] = 'El teléfono tiene 9 dígitos y empieza con 9';
    if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(f.email)) e['email'] = 'Correo inválido';
    if ((nuevo || f.clave) && !/^(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,64}$/.test(f.clave ?? '')) e['clave'] = '8+ caracteres con mayúscula, número y símbolo';
    if (!(f.tasaPorcentaje > 0)) e['tasaPorcentaje'] = 'La tasa debe ser mayor a 0';
    if (f.tasaMoraPorcentaje < 0) e['tasaMoraPorcentaje'] = 'No puede ser negativa';
    const t = this.topes();
    if (t && this.teaPrev() > t.teaCompensatoria * 100) e['tasaPorcentaje'] = `Supera el tope compensatorio del BCRP de ${(t.teaCompensatoria * 100).toFixed(2)} %`;
    if (t && this.teaMorPrev() > t.teaMoratoria * 100) e['tasaMoraPorcentaje'] = `Supera el tope moratorio del BCRP de ${(t.teaMoratoria * 100).toFixed(2)} %`;
    if (!(f.limiteCredito > 0)) e['limiteCredito'] = 'El límite debe ser mayor a 0';
    if (f.maxMeses < 2 || f.maxMeses > 24) e['maxMeses'] = 'Entre 2 y 24 cuotas';
    if (f.diaCorte < 1 || f.diaCorte > 28) e['diaCorte'] = 'Entre 1 y 28';
    if (f.diaPago < 1 || f.diaPago > 28) e['diaPago'] = 'Entre 1 y 28';
    if (f.diaCorte === f.diaPago) e['diaPago'] = 'El día de corte y el día de pago deben ser distintos';
    if (!f.aceptaPolitica) e['aceptaPolitica'] = 'El cliente debe aceptar la Política de Privacidad para registrarlo';
    return e;
  }

  guardar() {
    const f = this.form;
    const nuevo = this.modo() === 'nuevo';
    const e = this.validar();
    this.vivo.envio(e);
    if (Object.keys(e).length) { this.errorForm.set('Revisa los campos marcados'); return; }
    this.errorForm.set(null);
    this.guardando.set(true);
    const cuerpo = { ...f, diasCapitalizacion: f.tipoTasa === 'NOMINAL' || f.tipoTasaMora === 'NOMINAL' ? f.diasCapitalizacion : null, clave: f.clave || null };
    (nuevo ? this.api.crearCliente(cuerpo) : this.api.editarCliente(this.ficha()!.id, cuerpo)).subscribe({
      next: (r) => {
        this.guardando.set(false);
        this.ficha.set(r);
        this.modo.set('ver');
        this.avisos.ok(nuevo ? `${r.nombres} registrado. Su usuario para «Mi libreta» es su DNI.` : 'Condiciones actualizadas (solo afectan compras futuras)');
        if (nuevo) this.router.navigate(['/clientes', r.id], { replaceUrl: true });
      },
      error: (err) => { this.guardando.set(false); this.errores.set(erroresCampos(err)); this.errorForm.set(mensajeError(err)); },
    });
  }

  cancelar() {
    if (this.modo() === 'nuevo') this.router.navigate(['/clientes']);
    else this.modo.set('ver');
  }

  cambiarEstado() {
    const f = this.ficha()!;
    (f.estado === 'ACTIVO' ? this.api.bajaCliente(f.id) : this.api.reactivarCliente(f.id)).subscribe({
      next: (r) => { this.ficha.set(r); this.confirmaBaja.set(false); this.avisos.ok(r.estado === 'ACTIVO' ? 'Cliente reactivado' : 'Cliente dado de baja'); },
      error: (e) => { this.confirmaBaja.set(false); this.avisos.error(mensajeError(e)); },
    });
  }

  whatsapp() {
    const f = this.ficha()!;
    const r = f.resumen;
    const texto = `Hola ${f.nombres.split(' ')[0]}, te saluda ${this.auth.perfil()?.tienda?.razonSocial}. Tu libreta: saldo al corte S/ ${r.saldoAlCorte.toFixed(2)}`
      + (r.enMora ? ` + S/ ${r.moraAplicada.toFixed(2)} de mora (${r.diasMora} días)` : ` con pago hasta el ${r.fechaPago.split('-').reverse().join('/')}`)
      + `. Crédito disponible: S/ ${r.creditoLibre.toFixed(2)}. ¡Gracias por tu confianza!`;
    window.open(`https://wa.me/51${f.telefono}?text=${encodeURIComponent(texto)}`, '_blank', 'noopener');
  }

  hoy = new Date().toISOString().substring(0, 10);

  /** Imprime solo la hoja de confianza (siempre en colores claros, también desde el modo oscuro). */
  imprimir() {
    const html = document.documentElement;
    const oscuro = html.classList.contains('dark');
    html.classList.add('imprimir-hoja');
    if (oscuro) html.classList.remove('dark');
    const fin = () => { html.classList.remove('imprimir-hoja'); if (oscuro) html.classList.add('dark'); window.removeEventListener('afterprint', fin); };
    window.addEventListener('afterprint', fin);
    setTimeout(() => window.print(), 50);
  }
}
