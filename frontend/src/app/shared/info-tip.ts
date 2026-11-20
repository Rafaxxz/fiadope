import { Component, ElementRef, HostListener, input, signal } from '@angular/core';

/** Ícono ⓘ con la ayuda en línea del campo (qué es, formato y ejemplo). */
@Component({
  selector: 'fp-info',
  standalone: true,
  template: `
    <span class="relative inline-flex">
      <button type="button" class="p-1 text-on-surface-variant hover:text-primary transition-colors flex items-center"
              [attr.aria-label]="'Ayuda: ' + titulo()" (click)="abierto.set(!abierto()); $event.stopPropagation()">
        <span class="material-symbols-outlined text-lg">info</span>
      </button>
      @if (abierto()) {
        <span class="absolute right-0 top-7 w-64 p-3 bg-surface-container-highest text-on-surface text-body-sm font-body-sm rounded-lg shadow-[2px_3px_0px_#1c1b1b] z-30 text-left normal-case tracking-normal font-normal">
          <span class="block font-bold mb-1">{{ titulo() }}</span>
          <span class="block">{{ texto() }}</span>
        </span>
      }
    </span>`,
})
export class InfoTip {
  titulo = input.required<string>();
  texto = input.required<string>();
  abierto = signal(false);
  constructor(private el: ElementRef) {}
  @HostListener('document:click', ['$event'])
  fuera(e: Event) { if (!this.el.nativeElement.contains(e.target)) this.abierto.set(false); }
}

