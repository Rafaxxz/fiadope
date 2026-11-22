import { Component, computed, ElementRef, inject, OnDestroy, OnInit, QueryList, signal, ViewChildren } from '@angular/core';
import { Router } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { mensajeError } from '../../core/auth.interceptor';
import { AvisoService } from '../../shared/aviso';
import { AyudaModal } from '../../shared/ayuda-modal';

/** P02 – Verificación MFA (HU02). */
@Component({
  selector: 'fp-mfa',
  standalone: true,
  imports: [AyudaModal],
  templateUrl: './mfa.html',
})
export class MfaPage implements OnInit, OnDestroy {
  private api = inject(ApiService);
  private auth = inject(AuthService);
  private router = inject(Router);
  private avisos = inject(AvisoService);
  @ViewChildren('caja') cajas!: QueryList<ElementRef<HTMLInputElement>>;

  mfa = this.auth.mfa;
  digitos = signal<string[]>(['', '', '', '', '', '']);
  restante = signal(0);
  total = signal(300);
  error = signal<string | null>(null);
  enviando = signal(false);
  ayuda = signal(false);
  codigoDev = signal<string | null>(null);
  private reloj: ReturnType<typeof setInterval> | null = null;

  pasosMfa = [
    { titulo: 'Protección contra robos', texto: 'Si alguien adivina tu contraseña, no podrá ver las cuentas ni borrar los saldos sin el código que llega a tu correo.' },
    { titulo: 'Código de un solo uso', texto: 'El código tiene 6 dígitos y vence en 5 minutos. Si pides otro, el anterior deja de funcionar.' },
    { titulo: '¿No te llegó?', texto: 'Revisa tu bandeja de spam o presiona «Reenviar código al correo».' },
  ];

  tiempo = computed(() => {
    const s = Math.max(0, this.restante());
    return `${String(Math.floor(s / 60)).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`;
  });
  porcentaje = computed(() => (this.restante() / this.total()) * 100);
  completo = computed(() => this.digitos().every((d) => /^\d$/.test(d)));

  ngOnInit() {
    if (!this.mfa) { this.router.navigate(['/login']); return; }
    this.codigoDev.set(this.mfa.codigoDev);
    this.iniciarReloj(this.mfa.segundosVigencia);
  }
  ngOnDestroy() { if (this.reloj) clearInterval(this.reloj); }

  private iniciarReloj(segundos: number) {
    this.total.set(segundos);
    const fin = Date.now() + segundos * 1000;
    this.restante.set(segundos);
    if (this.reloj) clearInterval(this.reloj);
    this.reloj = setInterval(() => this.restante.set(Math.max(0, Math.round((fin - Date.now()) / 1000))), 1000);
  }

  escribir(i: number, e: Event) {
    const input = e.target as HTMLInputElement;
    const v = input.value.replace(/\D/g, '');
    if (v.length > 1) { this.pegar(v, i); return; }
    const d = [...this.digitos()];
    d[i] = v;
    this.digitos.set(d);
    input.value = v;
    if (v && i < 5) this.cajas.get(i + 1)?.nativeElement.focus();
    if (this.completo()) this.confirmar();
  }

  tecla(i: number, e: KeyboardEvent) {
    if (e.key === 'Backspace' && !this.digitos()[i] && i > 0) this.cajas.get(i - 1)?.nativeElement.focus();
  }

  pegar(texto: string, desde = 0) {
    const nums = texto.replace(/\D/g, '').slice(0, 6 - desde).split('');
    const d = [...this.digitos()];
    nums.forEach((n, k) => (d[desde + k] = n));
    this.digitos.set(d);
    this.cajas.forEach((c, k) => (c.nativeElement.value = d[k]));
    this.cajas.get(Math.min(5, desde + nums.length))?.nativeElement.focus();
    if (this.completo()) this.confirmar();
  }

  alPegar(e: ClipboardEvent) {
    e.preventDefault();
    this.pegar(e.clipboardData?.getData('text') ?? '');
  }

  confirmar() {
    if (!this.mfa || this.enviando()) return;
    if (!this.completo()) { this.error.set('Ingresa los 6 dígitos del código'); return; }
    if (this.restante() <= 0) { this.error.set('El código venció. Pide uno nuevo.'); return; }
    this.error.set(null);
    this.enviando.set(true);
    this.api.verificar(this.mfa.mfaToken, this.digitos().join('')).subscribe({
      next: (s) => {
        this.auth.iniciar(s);
        this.router.navigate([this.auth.inicio(s.perfil.rol)], { replaceUrl: true });
      },
      error: (e) => {
        this.enviando.set(false);
        this.error.set(mensajeError(e));
        this.digitos.set(['', '', '', '', '', '']);
        this.cajas.forEach((c) => (c.nativeElement.value = ''));
        this.cajas.first?.nativeElement.focus();
      },
    });
  }

  reenviar() {
    if (!this.mfa) return;
    this.api.reenviar(this.mfa.mfaToken).subscribe({
      next: (r) => {
        this.auth.guardarMfa(r, this.mfa!.usuario);
        this.mfa = this.auth.mfa;
        this.codigoDev.set(r.codigoDev);
        this.iniciarReloj(r.segundosVigencia);
        this.error.set(null);
        this.avisos.ok(`Enviamos un código nuevo a ${r.correoOculto}. El anterior ya no funciona.`);
      },
      error: (e) => this.error.set(mensajeError(e)),
    });
  }

  volver() {
    this.auth.limpiar();
    this.router.navigate(['/login'], { replaceUrl: true });
  }
}

