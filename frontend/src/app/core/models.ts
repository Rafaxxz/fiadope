export type Rol = 'ADMIN_PLATAFORMA' | 'ADMIN_NEGOCIO' | 'CLIENTE';
export type Modalidad = 'FIN_DE_MES' | 'CUOTAS';
export type TipoTasa = 'NOMINAL' | 'EFECTIVA';

export interface TiendaResumen { id: number; razonSocial: string; ruc: string; giro: string; ubicacion: string; propietario: string; limiteGlobal: number; }
export interface Perfil { id: number; login: string; nombre: string; correo: string; rol: Rol; tienda: TiendaResumen | null; idCliente: number | null; }
export interface LoginRespuesta { mfaToken: string; correoOculto: string; segundosVigencia: number; codigoDev: string | null; }
export interface SesionRespuesta { token: string; segundosInactividad: number; perfil: Perfil; }
export interface Captcha { captchaId: string; imagen: string; }

export interface Tienda { id: number; ruc: string; razonSocial: string; propietario: string; giro: string; ubicacion: string; direccion: string; telefono: string; limiteGlobal: number; limiteAsignado: number; estado: 'ACTIVA' | 'BAJA'; fechaAlta: string; clientes: number; adminLogin: string; adminCorreo: string; }
export interface Topes { teaCompensatoria: number; teaMoratoria: number; }

export interface Producto { id: number; nombre: string; categoria: string; sku: string; marca: string | null; proveedor: string | null; unidad: string; precioContado: number; precioLista: number; permiteFinMes: boolean; permiteCuotas: boolean; imagenUrl: string | null; estado: 'ACTIVO' | 'BAJA'; }

export interface ClienteResumen { id: number; dni: string; nombres: string; direccion: string; telefono: string; estadoCliente: string; limite: number; deudaVigente: number; creditoLibre: number; usoPorcentaje: number; saldoAlCorte: number; fechaCorte: string; fechaPago: string; diasMora: number; moraAplicada: number; totalACobrar: number; idEstadoPendiente: number | null; enMora: boolean; }
export interface Consentimiento { version: string; fechaHora: string; ip: string; }
export interface FilaCronograma { numero: number; vencimiento: string; saldoInicial: number; interes: number; amortizacion: number; cuota: number; saldoFinal: number; pagada: boolean; }
export interface Compra { id: number; idCliente: number; cliente: string; idProducto: number; producto: string; imagenUrl: string | null; fechaHora: string; cantidad: number; capital: number; modalidad: Modalidad; nCuotas: number | null; fechaCorte: string; fechaPagoAsignada: string; dias: number; montoS: number; interes: number; cuotaFija: number | null; totalPlazo: number; pagada: boolean; cuotasPagadas: number; proximaCuota: FilaCronograma | null; }
export interface ClienteFicha { id: number; dni: string; nombres: string; direccion: string; telefono: string; email: string; login: string; tipoTasa: TipoTasa; diasPlazoTasa: number; diasCapitalizacion: number | null; tasaPorcentaje: number; tipoTasaMora: TipoTasa; tasaMoraPorcentaje: number; teaPorcentaje: number; teaMoraPorcentaje: number; tedPorcentaje: number; temPorcentaje: number; moneda: string; limiteCredito: number; maxMeses: number; diaCorte: number; diaPago: number; estado: string; fechaRegistro: string; consentimiento: Consentimiento | null; resumen: ClienteResumen; compras: Compra[]; }
export interface ClienteRequest { dni: string; nombres: string; direccion: string; telefono: string; email: string; clave: string | null; tipoTasa: TipoTasa; diasPlazoTasa: number; diasCapitalizacion: number | null; tasaPorcentaje: number; tipoTasaMora: TipoTasa; tasaMoraPorcentaje: number; moneda: string; limiteCredito: number; maxMeses: number; diaCorte: number; diaPago: number; aceptaPolitica: boolean; }

export interface CompraRequest { idCliente: number; idProducto: number; cantidad: number; fechaHora: string | null; modalidad: Modalidad; nCuotas: number | null; }
export interface Simulacion { idCliente: number; cliente: string; idProducto: number; producto: string; cantidad: number; precioLista: number; capital: number; modalidad: Modalidad; nCuotas: number | null; fechaCompra: string; fechaCorte: string; fechaPago: string; despuesDelCorte: boolean; dias: number; tedPorcentaje: number; temPorcentaje: number; teaPorcentaje: number; montoS: number | null; interes: number; capitalAjustado: number | null; cuota: number | null; cronograma: FilaCronograma[]; totalVentaPlazo: number; interesTotal: number; limite: number; deudaVigente: number; creditoDisponible: number; excedeLimite: boolean; mensaje: string | null; maxCuotas: number; }
export interface Cronograma { compra: Compra; cliente: string; dni: string; teaPorcentaje: number; temPorcentaje: number; tedPorcentaje: number; diasGracia: number; capital: number; interesGracia: number; capitalAjustado: number; cuota: number; filas: FilaCronograma[]; totalInteres: number; totalAmortizacion: number; totalVentaPlazo: number; interesTotal: number; }

export interface Item { tipo: 'COMPRA' | 'CUOTA' | 'MORA'; detalle: string; fecha: string; dias: number; monto: number; interes: number; total: number; }
export interface CorteCliente { idCliente: number; nombres: string; dni: string; email: string; fechaCorte: string; fechaPago: string; items: Item[]; total: number; notas: string[]; generado: boolean; idEstado: number | null; estado: string | null; enviado: boolean; }
export interface CortePreview { anio: number; mes: number; totalPeriodo: number; clientesConSaldo: number; clientes: CorteCliente[]; }
export interface CorteGenerado { generados: number; omitidos: number; detalle: string[]; }

export interface PagoPreview { idEstado: number; idCliente: number; cliente: string; dni: string; fechaCorte: string; fechaPago: string; fechaPagoReal: string; items: Item[]; saldoAlCorte: number; diasMora: number; interesCompensatorioMora: number; interesMoratorio: number; totalMora: number; total: number; teaPorcentaje: number; teaMoraPorcentaje: number; moneda: string; estadosPendientes: number; }
export interface PagoRequest { idEstado: number; fechaPagoReal: string; metodoPago: string; nroOperacion: string | null; montoPagado: number; }
export interface Pago { id: number; idEstado: number; cliente: string; fechaReal: string; metodoPago: string; nroOperacion: string | null; diasMora: number; intCompMora: number; intMora: number; totalPagado: number; registrado: string; saldoLibreta: number | null; }

export interface ProductoTop { nombre: string; monto: number; }
export interface Panel { tienda: string; propietario: string; ubicacion: string; limiteGlobal: number; totalPorCobrar: number; clientesConSaldo: number; proximoCorte: string | null; proximoPago: string | null; diasParaCorte: number; montoProximoCorte: number; clientesEnMora: number; montoEnMora: number; teaBasePorcentaje: number; teaMoraBasePorcentaje: number; totalClientes: number; clientes: ClienteResumen[]; topProductos: ProductoTop[]; }

export interface EstadoCuenta { id: number; fechaCorte: string; fechaPago: string; total: number; estado: string; items: Item[]; pago: Pago | null; }
export interface MiCuenta { nombres: string; dni: string; tienda: string; moneda: string; totalAPagar: number; fechaLimite: string; diasMora: number; consumoMes: number; mora: number; lineaCredito: number; deudaVigente: number; creditoDisponible: number; usoPorcentaje: number; teaPorcentaje: number; teaMoraPorcentaje: number; diaCorte: number; diaPago: number; idEstadoPendiente: number | null; detalle: Item[]; compras: Compra[]; estados: EstadoCuenta[]; }

export interface RegistroBitacora { id: number; fechaHora: string; usuario: string | null; accion: string; tabla: string; idRegistro: number | null; valorAntes: string | null; valorDespues: string | null; ip: string; }
export interface Pagina<T> { contenido: T[]; pagina: number; tamano: number; total: number; totalPaginas: number; }

export interface ErrorApi { status: number; mensaje: string; errores: Record<string, string>; }

