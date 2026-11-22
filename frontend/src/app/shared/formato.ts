import { Pipe, PipeTransform } from '@angular/core';

const fmtSoles = new Intl.NumberFormat('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

/** 1234.5 -> "1,234.50" (formato de los mockups). */
export function soles(v: number | null | undefined): string {
  return fmtSoles.format(Number(v ?? 0));
}

/** "2026-09-26" -> "26/09/2026"; corto: "26/09". */
export function fecha(v: string | null | undefined, corto = false): string {
  if (!v) return '—';
  const [a, m, d] = v.substring(0, 10).split('-');
  return corto ? `${d}/${m}` : `${d}/${m}/${a}`;
}

export function hora(v: string | null | undefined): string {
  if (!v || v.length < 16) return '';
  return v.substring(11, 16);
}

export function iniciales(nombre: string): string {
  return nombre.split(/\s+/).filter(Boolean).slice(0, 2).map((p) => p[0]).join('').toUpperCase();
}

export function hoyIso(): string {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

export const MESES = ['enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio', 'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre'];

@Pipe({ name: 'soles', standalone: true })
export class SolesPipe implements PipeTransform {
  transform(v: number | null | undefined): string { return soles(v); }
}

@Pipe({ name: 'fecha', standalone: true })
export class FechaPipe implements PipeTransform {
  transform(v: string | null | undefined, corto = false): string { return fecha(v, corto); }
}

@Pipe({ name: 'hora', standalone: true })
export class HoraPipe implements PipeTransform {
  transform(v: string | null | undefined): string { return hora(v); }
}

@Pipe({ name: 'iniciales', standalone: true })
export class InicialesPipe implements PipeTransform {
  transform(v: string): string { return iniciales(v ?? ''); }
}

