import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ApiService } from '../../core/api.service';
import { mensajeError } from '../../core/auth.interceptor';
import { Cronograma, MiCuenta } from '../../core/models';
import { AvisoService } from '../../shared/aviso';
import { AyudaModal } from '../../shared/ayuda-modal';
import { AYUDAS } from '../../shared/ayudas';
import { FechaPipe, hoyIso, InicialesPipe, SolesPipe } from '../../shared/formato';
import { InfoTip } from '../../shared/info-tip';
import { ProductoImg } from '../../shared/producto-img';

/** P11 – Mi cuenta (HU11): portal del cliente pensado para el celular. */
import { Esqueleto } from '../../shared/esqueleto';
@Component({
  selector: 'fp-mi-cuenta',
  standalone: true,
  imports: [SolesPipe, FechaPipe, InicialesPipe, InfoTip, AyudaModal, ProductoImg, Esqueleto],
  templateUrl: './mi-cuenta.html',
})
export class MiCuentaPage implements OnInit {
  private api = inject(ApiService);
  private avisos = inject(AvisoService);

  ayudaMi = AYUDAS['micuenta'];
  cuenta = signal<MiCuenta | null>(null);
  crono = signal<Cronograma | null>(null);
  ayuda = signal(false);
  descargando = signal(false);
  hoy = hoyIso();

  nombre = computed(() => this.cuenta()?.nombres.split(' ')[0] ?? '');
  vencida = computed(() => {
    const c = this.cuenta();
    return !!c && !!c.idEstadoPendiente && c.fechaLimite < this.hoy;
  });
  activas = computed(() => (this.cuenta()?.compras ?? []).filter((c) => !c.pagada));
  pagadas = computed(() => (this.cuenta()?.compras ?? []).filter((c) => c.pagada));

  ngOnInit() {
    this.api.miCuenta().subscribe({ next: (c) => this.cuenta.set(c), error: (e) => this.avisos.error(mensajeError(e)) });
  }

  pdf(idEstado?: number) {
    this.descargando.set(true);
    this.api.descargar(this.api.miEstadoPdf(idEstado), `mi-estado-de-cuenta${idEstado ? '-' + idEstado : ''}.pdf`, (ok) => {
      this.descargando.set(false);
      if (!ok) this.avisos.error('Aún no tienes un estado de cuenta para descargar');
    });
  }

  verCronograma(id: number) {
    this.api.miCronograma(id).subscribe({ next: (c) => this.crono.set(c), error: (e) => this.avisos.error(mensajeError(e)) });
  }

  cronogramaPdf(id: number) {
    this.api.descargar(this.api.miCronogramaPdf(id), `mi-cronograma-${id}.pdf`);
  }
}

