import { BotonTema } from '../../shared/boton-tema';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { erroresCampos, mensajeError } from '../../core/auth.interceptor';
import { AyudaModal } from '../../shared/ayuda-modal';
import { AYUDAS } from '../../shared/ayudas';
import { InfoTip } from '../../shared/info-tip';

/** P01 – Login (HU01). */
@Component({
  selector: 'fp-login',
  standalone: true,
  imports: [FormsModule, InfoTip, AyudaModal, BotonTema],
  templateUrl: './login.html',
})
export class LoginPage implements OnInit {
  private api = inject(ApiService);
  private auth = inject(AuthService);
  private router = inject(Router);

  ayudaLogin = AYUDAS['login'];
  usuario = '';
  clave = signal('');
  captcha = '';
  captchaId = '';
  captchaImg = signal<string | null>(null);
  verClave = signal(false);
  ayuda = signal(false);
  olvido = signal(false);
  enviando = signal(false);
  error = signal<string | null>(null);
  errores = signal<Record<string, string>>({});
  aviso = this.auth.mensajeSalida;

  largo = computed(() => this.clave().length >= 8);
  mayusNum = computed(() => /[A-Z]/.test(this.clave()) && /[0-9]/.test(this.clave()));
  simbolo = computed(() => /[^A-Za-z0-9]/.test(this.clave()));
  puntaje = computed(() => {
    const v = this.clave();
    let s = 0;
    if (v.length > 0) s++;
    if (this.largo() && (this.mayusNum() || this.simbolo())) s++;
    if (this.largo() && this.mayusNum() && this.simbolo()) s++;
    return s;
  });
  fortaleza = computed(() => [
    { texto: 'Vacía', clase: 'text-on-surface-variant', barras: ['bg-surface-dim', 'bg-surface-dim', 'bg-surface-dim'] },
    { texto: 'Débil', clase: 'text-error', barras: ['bg-error', 'bg-surface-dim', 'bg-surface-dim'] },
    { texto: 'Media', clase: 'text-secondary-container', barras: ['bg-secondary-container', 'bg-secondary-container', 'bg-surface-dim'] },
    { texto: 'Fuerte (Recomendada)', clase: 'text-secondary', barras: ['bg-secondary', 'bg-secondary', 'bg-secondary'] },
  ][this.puntaje()]);

  ngOnInit() { this.nuevoCaptcha(); }

  nuevoCaptcha() {
    this.captcha = '';
    this.api.captcha().subscribe({
      next: (c) => { this.captchaId = c.captchaId; this.captchaImg.set(c.imagen); },
      error: (e) => this.error.set(mensajeError(e)),
    });
  }

  ingresar() {
    this.error.set(null);
    const errs: Record<string, string> = {};
    if (!/^(\d{8}|\d{11})$/.test(this.usuario.trim())) errs['usuario'] = 'Debe ser un DNI de 8 dígitos o un RUC de 11';
    if (this.puntaje() < 3) errs['contrasena'] = 'La contraseña es débil: necesita 8 caracteres, una mayúscula, un número y un símbolo';
    if (!/^\d{4}$/.test(this.captcha.trim())) errs['captcha'] = 'Escribe los 4 números de la imagen';
    this.errores.set(errs);
    if (Object.keys(errs).length) return;
    this.enviando.set(true);
    this.api.login({ usuario: this.usuario.trim(), contrasena: this.clave(), captchaId: this.captchaId, captcha: this.captcha.trim() }).subscribe({
      next: (r) => {
        this.auth.guardarMfa(r, this.usuario.trim());
        this.auth.mensajeSalida.set(null);
        this.router.navigate(['/verificar']);
      },
      error: (e) => {
        this.enviando.set(false);
        this.error.set(mensajeError(e));
        this.errores.set(erroresCampos(e));
        this.nuevoCaptcha();
      },
    });
  }
}

