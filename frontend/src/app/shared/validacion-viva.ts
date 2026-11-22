import { signal, WritableSignal } from '@angular/core';

/**
 * Validación mientras se escribe. Se engancha al <form> con:
 *   (focusout)="vivo.tocar($event)" (input)="vivo.revisar($event)" (change)="vivo.revisar($event)"
 * Un campo se valida desde que el usuario sale de él (o desde el primer intento de guardar);
 * a partir de ahí se marca en rojo con su mensaje o en verde cuando ya está bien.
 * `alias` traduce el name del input a la clave de error cuando no coinciden.
 */
export class ValidacionViva {
  private tocados = new Set<string>();
  private enviado = false;
  private form: HTMLFormElement | null = null;

  constructor(
    private validar: () => Record<string, string>,
    private errores: WritableSignal<Record<string, string>>,
    private alias: Record<string, string> = {},
  ) {}

  readonly activo = signal(true);

  private clave(el: Element | null): string | null {
    const n = el?.getAttribute?.('name');
    return n ? this.alias[n] ?? n : null;
  }

  tocar(ev: Event) {
    const k = this.clave(ev.target as Element);
    this.form = (ev.currentTarget as HTMLFormElement) ?? this.form;
    if (!k) return;
    this.tocados.add(k);
    this.revisar(ev);
  }

  revisar(ev?: Event) {
    if (ev) this.form = (ev.currentTarget as HTMLFormElement) ?? this.form;
    // ngModel ya actualizó el modelo en el evento del propio input; esperamos un tick por seguridad
    setTimeout(() => {
      const e = this.validar();
      const visibles: Record<string, string> = {};
      for (const k of Object.keys(e)) if (this.enviado || this.tocados.has(k)) visibles[k] = e[k];
      this.errores.set(visibles);
      this.pintar(e);
    });
  }

  /** Llamar al intentar guardar: desde ahí se muestran todos los errores y se siguen corrigiendo en vivo. */
  envio(e: Record<string, string>) { this.enviado = true; this.errores.set(e); setTimeout(() => this.pintar(e)); }

  reiniciar() {
    this.tocados.clear(); this.enviado = false;
    this.form?.querySelectorAll('.valido').forEach((x) => x.classList.remove('valido'));
  }

  private pintar(e: Record<string, string>) {
    this.form?.querySelectorAll<HTMLInputElement>('input[name], select[name], textarea[name]').forEach((el) => {
      const k = this.clave(el);
      const ok = !!k && (this.enviado || this.tocados.has(k)) && !e[k] && el.type !== 'checkbox' && el.type !== 'radio' && !!String(el.value ?? '').trim() && !el.readOnly;
      el.classList.toggle('valido', ok);
    });
  }
}
