import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import * as M from './models';

/** Cliente de la API REST de FiadoPe. */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private http = inject(HttpClient);
  readonly base = environment.apiUrl;

  private url(p: string) { return `${this.base}/api${p}`; }
  private params(o: Record<string, unknown>) {
    let p = new HttpParams();
    Object.entries(o).forEach(([k, v]) => { if (v !== null && v !== undefined && v !== '') p = p.set(k, String(v)); });
    return p;
  }

  /** Resuelve la URL de una imagen: subida al backend (/uploads) o del front (assets). */
  imagen(url: string | null | undefined): string | null {
    if (!url) return null;
    if (url.startsWith('/uploads')) return this.base + url;
    return url;
  }

  // P01-P02
  captcha() { return this.http.post<M.Captcha>(this.url('/auth/captcha'), {}); }
  login(b: { usuario: string; contrasena: string; captchaId: string; captcha: string }) { return this.http.post<M.LoginRespuesta>(this.url('/auth/login'), b); }
  verificar(mfaToken: string, codigo: string) { return this.http.post<M.SesionRespuesta>(this.url('/auth/mfa/verificar'), { mfaToken, codigo }); }
  reenviar(mfaToken: string) { return this.http.post<M.LoginRespuesta>(this.url('/auth/mfa/reenviar'), { mfaToken }); }
  yo() { return this.http.get<M.Perfil>(this.url('/auth/yo')); }
  logout() { return this.http.post<void>(this.url('/auth/logout'), {}); }

  // P03
  tiendas(f: { q?: string; giro?: string; estado?: string }) { return this.http.get<M.Tienda[]>(this.url('/admin/tiendas'), { params: this.params(f) }); }
  crearTienda(b: unknown) { return this.http.post<M.Tienda>(this.url('/admin/tiendas'), b); }
  editarTienda(id: number, b: unknown) { return this.http.put<M.Tienda>(this.url(`/admin/tiendas/${id}`), b); }
  bajaTienda(id: number) { return this.http.patch<M.Tienda>(this.url(`/admin/tiendas/${id}/baja`), {}); }
  reactivarTienda(id: number) { return this.http.patch<M.Tienda>(this.url(`/admin/tiendas/${id}/reactivar`), {}); }
  topesAdmin() { return this.http.get<M.Topes>(this.url('/admin/topes')); }
  guardarTopes(t: M.Topes) { return this.http.put<M.Topes>(this.url('/admin/topes'), t); }

  // P04
  panel() { return this.http.get<M.Panel>(this.url('/panel')); }
  topes() { return this.http.get<M.Topes>(this.url('/topes')); }

  // P05
  productos(f: { categoria?: string; q?: string; estado?: string } = {}) { return this.http.get<M.Producto[]>(this.url('/productos'), { params: this.params(f) }); }
  crearProducto(b: unknown) { return this.http.post<M.Producto>(this.url('/productos'), b); }
  editarProducto(id: number, b: unknown) { return this.http.put<M.Producto>(this.url(`/productos/${id}`), b); }
  bajaProducto(id: number) { return this.http.patch<M.Producto>(this.url(`/productos/${id}/baja`), {}); }
  reactivarProducto(id: number) { return this.http.patch<M.Producto>(this.url(`/productos/${id}/reactivar`), {}); }
  subirImagen(archivo: File) {
    const fd = new FormData();
    fd.append('archivo', archivo);
    return this.http.post<{ url: string }>(this.url('/productos/imagen'), fd);
  }

  // P06
  clientes(q?: string) { return this.http.get<M.ClienteResumen[]>(this.url('/clientes'), { params: this.params({ q }) }); }
  cliente(id: number) { return this.http.get<M.ClienteFicha>(this.url(`/clientes/${id}`)); }
  crearCliente(b: M.ClienteRequest) { return this.http.post<M.ClienteFicha>(this.url('/clientes'), b); }
  editarCliente(id: number, b: M.ClienteRequest) { return this.http.put<M.ClienteFicha>(this.url(`/clientes/${id}`), b); }
  bajaCliente(id: number) { return this.http.patch<M.ClienteFicha>(this.url(`/clientes/${id}/baja`), {}); }
  reactivarCliente(id: number) { return this.http.patch<M.ClienteFicha>(this.url(`/clientes/${id}/reactivar`), {}); }
  comprasCliente(id: number) { return this.http.get<M.Compra[]>(this.url(`/clientes/${id}/compras`)); }

  // P07-P08
  simular(b: M.CompraRequest) { return this.http.post<M.Simulacion>(this.url('/compras/simular'), b); }
  registrarCompra(b: M.CompraRequest) { return this.http.post<M.Compra>(this.url('/compras'), b); }
  cronograma(id: number) { return this.http.get<M.Cronograma>(this.url(`/compras/${id}/cronograma`)); }
  cronogramaPdf(id: number) { return this.http.get(this.url(`/compras/${id}/cronograma/pdf`), { responseType: 'blob' }); }

  // P09
  corte(anio: number, mes: number) { return this.http.get<M.CortePreview>(this.url('/cortes'), { params: this.params({ anio, mes }) }); }
  generarCorte(anio: number, mes: number, idsClientes: number[]) { return this.http.post<M.CorteGenerado>(this.url('/cortes/generar'), { anio, mes, idsClientes }); }
  estadoPdf(id: number) { return this.http.get(this.url(`/estados-cuenta/${id}/pdf`), { responseType: 'blob' }); }

  // P10
  previewPago(f: { idCliente?: number; idEstado?: number; fecha?: string }) { return this.http.get<M.PagoPreview>(this.url('/pagos/preview'), { params: this.params(f) }); }
  pagar(b: M.PagoRequest) { return this.http.post<M.Pago>(this.url('/pagos'), b); }

  // P11
  miCuenta() { return this.http.get<M.MiCuenta>(this.url('/mi-cuenta')); }
  miCronograma(id: number) { return this.http.get<M.Cronograma>(this.url(`/mi-cuenta/compras/${id}/cronograma`)); }
  miCronogramaPdf(id: number) { return this.http.get(this.url(`/mi-cuenta/compras/${id}/cronograma/pdf`), { responseType: 'blob' }); }
  miEstadoPdf(idEstado?: number) { return this.http.get(this.url('/mi-cuenta/estado-cuenta/pdf'), { params: this.params({ idEstado }), responseType: 'blob' }); }

  // P12
  bitacora(f: Record<string, unknown>) { return this.http.get<M.Pagina<M.RegistroBitacora>>(this.url('/bitacora'), { params: this.params(f) }); }
  accionesBitacora() { return this.http.get<string[]>(this.url('/bitacora/acciones')); }

  /** Descarga un blob como archivo. */
  descargar(obs: Observable<Blob>, nombre: string, alTerminar?: (ok: boolean) => void) {
    obs.subscribe({
      next: (blob) => {
        const a = document.createElement('a');
        a.href = URL.createObjectURL(blob);
        a.download = nombre;
        a.click();
        setTimeout(() => URL.revokeObjectURL(a.href), 2000);
        alTerminar?.(true);
      },
      error: () => alTerminar?.(false),
    });
  }
}

