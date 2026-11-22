import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { erroresCampos, mensajeError } from '../../core/auth.interceptor';
import { Producto } from '../../core/models';
import { AvisoService } from '../../shared/aviso';
import { AyudaModal } from '../../shared/ayuda-modal';
import { AYUDAS } from '../../shared/ayudas';
import { SolesPipe } from '../../shared/formato';
import { InfoTip } from '../../shared/info-tip';
import { ProductoImg } from '../../shared/producto-img';

export const CATEGORIAS = [
  { valor: 'Abarrotes y granel', emoji: '🍚' },
  { valor: 'Lácteos y panadería', emoji: '🥛' },
  { valor: 'Bebidas y gaseosas', emoji: '🥤' },
  { valor: 'Electro y hogar', emoji: '🍳' },
  { valor: 'Otro', emoji: '🧺' },
];

interface FormProducto { nombre: string; categoria: string; sku: string; marca: string; proveedor: string; unidad: string; precioContado: number | null; precioLista: number | null; permiteFinMes: boolean; permiteCuotas: boolean; imagenUrl: string | null; }

/** P05 – Catálogo de productos (HU05). */
import { Esqueleto } from '../../shared/esqueleto';
import { ValidacionViva } from '../../shared/validacion-viva';
@Component({
  selector: 'fp-catalogo',
  standalone: true,
  imports: [FormsModule, RouterLink, SolesPipe, InfoTip, AyudaModal, ProductoImg, Esqueleto],
  templateUrl: './catalogo.html',
})
export class CatalogoPage implements OnInit {
  private api = inject(ApiService);
  private avisos = inject(AvisoService);
  auth = inject(AuthService);

  categorias = CATEGORIAS;
  ayudaCatalogo = AYUDAS['catalogo'];
  productos = signal<Producto[]>([]);
  cargando = signal(true);
  categoria = signal<string>('');
  q = signal('');
  verBajas = signal(false);
  ayuda = signal(false);
  modal = signal(false);
  editando = signal<Producto | null>(null);
  form: FormProducto = this.vacio();
  errores = signal<Record<string, string>>({});
  vivo = new ValidacionViva(() => this.validar(), this.errores, { finmes: 'modalidades', cuotas: 'modalidades' });
  errorForm = signal<string | null>(null);
  guardando = signal(false);
  subiendo = signal(false);

  filtrados = computed(() => {
    const q = this.q().toLowerCase().trim();
    return this.productos().filter((p) =>
      (this.verBajas() ? p.estado === 'BAJA' : p.estado === 'ACTIVO') &&
      (!this.categoria() || p.categoria === this.categoria()) &&
      (!q || p.nombre.toLowerCase().includes(q) || p.sku.toLowerCase().includes(q)));
  });
  activos = computed(() => this.productos().filter((p) => p.estado === 'ACTIVO'));
  cuenta = (c: string) => this.activos().filter((p) => p.categoria === c).length;
  margenPromedio = computed(() => {
    const a = this.activos();
    if (!a.length) return 0;
    return a.reduce((s, p) => s + ((p.precioLista - p.precioContado) * 100) / p.precioContado, 0) / a.length;
  });
  enCuotas = computed(() => this.activos().filter((p) => p.permiteCuotas).length);

  ngOnInit() { this.cargar(); }

  cargar() {
    this.api.productos().subscribe({ next: (p) => { this.productos.set(p); this.cargando.set(false); }, error: (e) => { this.cargando.set(false); this.avisos.error(mensajeError(e)); } });
  }

  emoji(c: string) { return CATEGORIAS.find((x) => x.valor === c)?.emoji ?? '🧺'; }
  margen(p: Producto) { return p.precioLista - p.precioContado; }

  vacio(): FormProducto {
    return { nombre: '', categoria: 'Abarrotes y granel', sku: '', marca: '', proveedor: '', unidad: 'UND', precioContado: null, precioLista: null, permiteFinMes: true, permiteCuotas: false, imagenUrl: null };
  }

  nuevo() { this.form = this.vacio(); this.vivo.reiniciar(); this.editando.set(null); this.errores.set({}); this.errorForm.set(null); this.modal.set(true); }

  editar(p: Producto) {
    this.form = { nombre: p.nombre, categoria: p.categoria, sku: p.sku, marca: p.marca ?? '', proveedor: p.proveedor ?? '', unidad: p.unidad, precioContado: p.precioContado, precioLista: p.precioLista, permiteFinMes: p.permiteFinMes, permiteCuotas: p.permiteCuotas, imagenUrl: p.imagenUrl };
    this.editando.set(p); this.vivo.reiniciar(); this.errores.set({}); this.errorForm.set(null); this.modal.set(true);
  }

  elegirImagen(ev: Event) {
    const archivo = (ev.target as HTMLInputElement).files?.[0];
    if (!archivo) return;
    if (!['image/jpeg', 'image/png'].includes(archivo.type) || archivo.size > 2 * 1024 * 1024) {
      this.errores.set({ ...this.errores(), imagen: 'Solo imágenes JPG o PNG de hasta 2 MB' });
      return;
    }
    this.subiendo.set(true);
    this.api.subirImagen(archivo).subscribe({
      next: (r) => { this.form.imagenUrl = r.url; this.subiendo.set(false); const e = { ...this.errores() }; delete e['imagen']; this.errores.set(e); },
      error: (e) => { this.subiendo.set(false); this.errores.set({ ...this.errores(), imagen: mensajeError(e) }); },
    });
  }

  validar(): Record<string, string> {
    const f = this.form;
    const e: Record<string, string> = {};
    if (!f.nombre.trim()) e['nombre'] = 'Ingresa el nombre';
    if (!f.sku.trim()) e['sku'] = 'Ingresa el SKU';
    if (!f.precioContado || f.precioContado <= 0) e['precioContado'] = 'El precio contado debe ser mayor a 0';
    if (f.precioLista === null || f.precioLista === undefined) e['precioLista'] = 'Ingresa el precio de lista';
    else if (f.precioContado && f.precioLista < f.precioContado) e['precioLista'] = 'El precio de lista debe ser mayor o igual al precio contado';
    if (!f.permiteFinMes && !f.permiteCuotas) e['modalidades'] = 'Marca al menos una modalidad: fin de mes o cuotas';
    return e;
  }

  guardar() {
    const f = this.form;
    const e = this.validar();
    this.vivo.envio(e);
    if (Object.keys(e).length) return;
    this.guardando.set(true);
    const p = this.editando();
    (p ? this.api.editarProducto(p.id, f) : this.api.crearProducto(f)).subscribe({
      next: (r) => { this.guardando.set(false); this.modal.set(false); this.avisos.ok(p ? `${r.nombre} actualizado` : `${r.nombre} agregado al catálogo`); this.cargar(); },
      error: (err) => { this.guardando.set(false); this.errores.set(erroresCampos(err)); this.errorForm.set(mensajeError(err)); },
    });
  }

  cambiarEstado(p: Producto) {
    (p.estado === 'ACTIVO' ? this.api.bajaProducto(p.id) : this.api.reactivarProducto(p.id)).subscribe({
      next: () => { this.avisos.ok(p.estado === 'ACTIVO' ? `${p.nombre} dado de baja: ya no aparece en compras nuevas` : `${p.nombre} reactivado`); this.cargar(); },
      error: (e) => this.avisos.error(mensajeError(e)),
    });
  }
}

