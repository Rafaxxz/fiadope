package pe.fiadope.api.dto;

import jakarta.validation.constraints.*;
import pe.fiadope.api.domain.Modalidad;
import pe.fiadope.api.domain.Rol;
import pe.fiadope.api.finanzas.MotorFinanciero.TipoTasa;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** DTOs de la API agrupados por pantalla (P01 a P12). */
public final class Dtos {

    private Dtos() {
    }

    // ------------------------------------------------------------ P01 / P02
    public record LoginRequest(
            @NotBlank(message = "Ingresa tu usuario")
            @Pattern(regexp = "^([0-9]{8}|[0-9]{11})$", message = "Debe ser un DNI de 8 dígitos o un RUC de 11")
            String usuario,
            @NotBlank(message = "Ingresa tu contraseña") @Size(max = 64) String contrasena,
            @NotBlank(message = "Vuelve a cargar el captcha") String captchaId,
            @NotBlank(message = "Escribe el código de la imagen") @Pattern(regexp = "^[0-9]{4}$", message = "El captcha tiene 4 dígitos") String captcha) {
    }

    public record LoginRespuesta(String mfaToken, String correoOculto, int segundosVigencia, String codigoDev) {
    }

    public record MfaRequest(@NotBlank String mfaToken,
                             @NotBlank(message = "Ingresa el código") @Pattern(regexp = "^[0-9]{6}$", message = "El código tiene 6 dígitos") String codigo) {
    }

    public record MfaReenvioRequest(@NotBlank String mfaToken) {
    }

    public record TiendaResumen(Integer id, String razonSocial, String ruc, String giro, String ubicacion,
                                String propietario, BigDecimal limiteGlobal) {
    }

    public record PerfilDto(Integer id, String login, String nombre, String correo, Rol rol, TiendaResumen tienda,
                            Integer idCliente) {
    }

    public record SesionRespuesta(String token, int segundosInactividad, PerfilDto perfil) {
    }

    // ------------------------------------------------------------------ P03
    public record TopesDto(BigDecimal teaCompensatoria, BigDecimal teaMoratoria) {
    }

    public record TiendaRequest(
            @NotBlank @Pattern(regexp = "^(10|20)[0-9]{9}$", message = "El RUC tiene 11 dígitos y empieza con 10 o 20") String ruc,
            @NotBlank(message = "Ingresa el nombre de la tienda") @Size(max = 120) String razonSocial,
            @NotBlank(message = "Ingresa el propietario") @Size(max = 100) String propietario,
            @NotBlank(message = "Elige el giro") @Size(max = 30) String giro,
            @NotBlank(message = "Ingresa el distrito") @Size(max = 60) String ubicacion,
            @NotBlank(message = "Ingresa la dirección") @Size(max = 150) String direccion,
            @NotBlank @Pattern(regexp = "^9[0-9]{8}$", message = "El teléfono tiene 9 dígitos y empieza con 9") String telefono,
            @NotNull(message = "Ingresa el límite global") @DecimalMin(value = "0.01", message = "El límite debe ser mayor a 0") @Digits(integer = 10, fraction = 2) BigDecimal limiteGlobal,
            @NotBlank(message = "Ingresa el correo del administrador") @Email(message = "Correo inválido") @Size(max = 100) String adminEmail,
            String adminClave) {
    }

    public record TiendaDto(Integer id, String ruc, String razonSocial, String propietario, String giro,
                            String ubicacion, String direccion, String telefono, BigDecimal limiteGlobal,
                            BigDecimal limiteAsignado, String estado, LocalDateTime fechaAlta, long clientes,
                            String adminLogin, String adminCorreo) {
    }

    // ------------------------------------------------------------------ P05
    public record ProductoRequest(
            @NotBlank(message = "Ingresa el nombre") @Size(max = 100) String nombre,
            @NotBlank(message = "Elige la categoría") @Size(max = 30) String categoria,
            @NotBlank(message = "Ingresa el SKU") @Size(max = 15) String sku,
            @Size(max = 60) String marca,
            @Size(max = 60) String proveedor,
            @Size(max = 20) String unidad,
            @NotNull(message = "Ingresa el precio contado") @DecimalMin(value = "0.01", message = "El precio contado debe ser mayor a 0") @Digits(integer = 10, fraction = 2) BigDecimal precioContado,
            @NotNull(message = "Ingresa el precio de lista") @Digits(integer = 10, fraction = 2) BigDecimal precioLista,
            boolean permiteFinMes,
            boolean permiteCuotas,
            @Size(max = 255) String imagenUrl) {
    }

    public record ProductoDto(Integer id, String nombre, String categoria, String sku, String marca, String proveedor,
                              String unidad, BigDecimal precioContado, BigDecimal precioLista, boolean permiteFinMes,
                              boolean permiteCuotas, String imagenUrl, String estado) {
    }

    // ------------------------------------------------------------------ P06
    public record ClienteRequest(
            @NotBlank @Pattern(regexp = "^[0-9]{8,12}$", message = "DNI de 8 dígitos o CE de 9 a 12") String dni,
            @NotBlank(message = "Ingresa los nombres") @Size(max = 100) String nombres,
            @NotBlank(message = "Ingresa la dirección") @Size(max = 150) String direccion,
            @NotBlank @Pattern(regexp = "^9[0-9]{8}$", message = "El teléfono tiene 9 dígitos y empieza con 9") String telefono,
            @NotBlank(message = "Ingresa el correo") @Email(message = "Correo inválido") @Size(max = 100) String email,
            String clave,
            @NotNull TipoTasa tipoTasa,
            @NotNull @Min(1) @Max(360) Integer diasPlazoTasa,
            @Min(1) @Max(360) Integer diasCapitalizacion,
            @NotNull(message = "Ingresa la tasa compensatoria") @DecimalMin(value = "0.0000001", message = "La tasa debe ser mayor a 0") BigDecimal tasaPorcentaje,
            @NotNull TipoTasa tipoTasaMora,
            @NotNull @DecimalMin(value = "0", message = "La tasa moratoria no puede ser negativa") BigDecimal tasaMoraPorcentaje,
            @NotBlank @Pattern(regexp = "^(PEN|USD)$") String moneda,
            @NotNull @DecimalMin(value = "0.01", message = "El límite debe ser mayor a 0") @Digits(integer = 10, fraction = 2) BigDecimal limiteCredito,
            @NotNull @Min(value = 2, message = "Mínimo 2 cuotas") @Max(value = 24, message = "Máximo 24 cuotas") Integer maxMeses,
            @NotNull @Min(1) @Max(28) Integer diaCorte,
            @NotNull @Min(1) @Max(28) Integer diaPago,
            boolean aceptaPolitica) {
    }

    public record ConsentimientoDto(String version, LocalDateTime fechaHora, String ip) {
    }

    public record ClienteResumenDto(Integer id, String dni, String nombres, String direccion, String telefono,
                                    String estadoCliente, BigDecimal limite, BigDecimal deudaVigente,
                                    BigDecimal creditoLibre, BigDecimal usoPorcentaje, BigDecimal saldoAlCorte,
                                    LocalDate fechaCorte, LocalDate fechaPago, int diasMora, BigDecimal moraAplicada,
                                    BigDecimal totalACobrar, Integer idEstadoPendiente, boolean enMora) {
    }

    public record ClienteFichaDto(Integer id, String dni, String nombres, String direccion, String telefono,
                                  String email, String login, TipoTasa tipoTasa, int diasPlazoTasa,
                                  Integer diasCapitalizacion, BigDecimal tasaPorcentaje, TipoTasa tipoTasaMora,
                                  BigDecimal tasaMoraPorcentaje, BigDecimal teaPorcentaje,
                                  BigDecimal teaMoraPorcentaje, BigDecimal tedPorcentaje, BigDecimal temPorcentaje,
                                  String moneda, BigDecimal limiteCredito, int maxMeses, int diaCorte, int diaPago,
                                  String estado, LocalDateTime fechaRegistro, ConsentimientoDto consentimiento,
                                  ClienteResumenDto resumen, List<CompraDto> compras) {
    }

    // ------------------------------------------------------------ P07 / P08
    public record CompraRequest(
            @NotNull(message = "Elige al cliente") Integer idCliente,
            @NotNull(message = "Elige el producto") Integer idProducto,
            @NotNull @DecimalMin(value = "0.001", message = "La cantidad debe ser mayor a 0") @Digits(integer = 7, fraction = 3) BigDecimal cantidad,
            LocalDateTime fechaHora,
            @NotNull(message = "Elige la modalidad") Modalidad modalidad,
            Integer nCuotas) {
    }

    public record FilaCronogramaDto(int numero, LocalDate vencimiento, BigDecimal saldoInicial, BigDecimal interes,
                                    BigDecimal amortizacion, BigDecimal cuota, BigDecimal saldoFinal, boolean pagada) {
    }

    public record SimulacionDto(Integer idCliente, String cliente, Integer idProducto, String producto,
                                BigDecimal cantidad, BigDecimal precioLista, BigDecimal capital, Modalidad modalidad,
                                Integer nCuotas, LocalDate fechaCompra, LocalDate fechaCorte, LocalDate fechaPago,
                                boolean despuesDelCorte, int dias, BigDecimal tedPorcentaje,
                                BigDecimal temPorcentaje, BigDecimal teaPorcentaje, BigDecimal montoS,
                                BigDecimal interes, BigDecimal capitalAjustado, BigDecimal cuota,
                                List<FilaCronogramaDto> cronograma, BigDecimal totalVentaPlazo,
                                BigDecimal interesTotal, BigDecimal limite, BigDecimal deudaVigente,
                                BigDecimal creditoDisponible, boolean excedeLimite, String mensaje,
                                int maxCuotas) {
    }

    public record CompraDto(Integer id, Integer idCliente, String cliente, Integer idProducto, String producto,
                            String imagenUrl, LocalDateTime fechaHora, BigDecimal cantidad, BigDecimal capital,
                            Modalidad modalidad, Integer nCuotas, LocalDate fechaCorte, LocalDate fechaPagoAsignada,
                            int dias, BigDecimal montoS, BigDecimal interes, BigDecimal cuotaFija,
                            BigDecimal totalPlazo, boolean pagada, int cuotasPagadas,
                            FilaCronogramaDto proximaCuota) {
    }

    public record CronogramaDto(CompraDto compra, String cliente, String dni, BigDecimal teaPorcentaje,
                                BigDecimal temPorcentaje, BigDecimal tedPorcentaje, int diasGracia,
                                BigDecimal capital, BigDecimal interesGracia, BigDecimal capitalAjustado,
                                BigDecimal cuota, List<FilaCronogramaDto> filas, BigDecimal totalInteres,
                                BigDecimal totalAmortizacion, BigDecimal totalVentaPlazo, BigDecimal interesTotal) {
    }

    // ------------------------------------------------------------------ P09
    public record ItemDto(String tipo, String detalle, LocalDate fecha, int dias, BigDecimal monto,
                          BigDecimal interes, BigDecimal total) {
    }

    public record CorteClienteDto(Integer idCliente, String nombres, String dni, String email,
                                  LocalDate fechaCorte, LocalDate fechaPago, List<ItemDto> items,
                                  BigDecimal total, List<String> notas, boolean generado, Integer idEstado,
                                  String estado, boolean enviado) {
    }

    public record CortePreviewDto(int anio, int mes, BigDecimal totalPeriodo, int clientesConSaldo,
                                  List<CorteClienteDto> clientes) {
    }

    public record CorteGenerarRequest(@NotNull @Min(2020) @Max(2100) Integer anio,
                                      @NotNull @Min(1) @Max(12) Integer mes,
                                      @NotEmpty(message = "Selecciona al menos un cliente") List<Integer> idsClientes) {
    }

    public record CorteGeneradoDto(int generados, int omitidos, List<String> detalle) {
    }

    // ------------------------------------------------------------------ P10
    public record PagoPreviewDto(Integer idEstado, Integer idCliente, String cliente, String dni,
                                 LocalDate fechaCorte, LocalDate fechaPago, LocalDate fechaPagoReal,
                                 List<ItemDto> items, BigDecimal saldoAlCorte, int diasMora,
                                 BigDecimal interesCompensatorioMora, BigDecimal interesMoratorio,
                                 BigDecimal totalMora, BigDecimal total, BigDecimal teaPorcentaje,
                                 BigDecimal teaMoraPorcentaje, String moneda, int estadosPendientes) {
    }

    public record PagoRequest(@NotNull Integer idEstado,
                              @NotNull(message = "Ingresa la fecha de pago") LocalDate fechaPagoReal,
                              @NotBlank @Pattern(regexp = "^(Yape/Plin|Efectivo|Transferencia)$", message = "Método de pago inválido") String metodoPago,
                              @Size(max = 20, message = "Máximo 20 caracteres") String nroOperacion,
                              @NotNull(message = "Ingresa el monto") BigDecimal montoPagado) {
    }

    public record PagoDto(Integer id, Integer idEstado, String cliente, LocalDate fechaReal, String metodoPago,
                          String nroOperacion, int diasMora, BigDecimal intCompMora, BigDecimal intMora,
                          BigDecimal totalPagado, LocalDateTime registrado, BigDecimal saldoLibreta) {
    }

    // ------------------------------------------------------------------ P04
    public record ProductoTopDto(String nombre, BigDecimal monto) {
    }

    public record PanelDto(String tienda, String propietario, String ubicacion, BigDecimal limiteGlobal,
                           BigDecimal totalPorCobrar, int clientesConSaldo, LocalDate proximoCorte,
                           LocalDate proximoPago, int diasParaCorte, BigDecimal montoProximoCorte,
                           int clientesEnMora, BigDecimal montoEnMora, BigDecimal teaBasePorcentaje,
                           BigDecimal teaMoraBasePorcentaje, int totalClientes,
                           List<ClienteResumenDto> clientes, List<ProductoTopDto> topProductos) {
    }

    // ------------------------------------------------------------------ P11
    public record EstadoCuentaDto(Integer id, LocalDate fechaCorte, LocalDate fechaPago, BigDecimal total,
                                  String estado, List<ItemDto> items, PagoDto pago) {
    }

    public record MiCuentaDto(String nombres, String dni, String tienda, String moneda, BigDecimal totalAPagar,
                              LocalDate fechaLimite, int diasMora, BigDecimal consumoMes, BigDecimal mora,
                              BigDecimal lineaCredito, BigDecimal deudaVigente, BigDecimal creditoDisponible,
                              BigDecimal usoPorcentaje, BigDecimal teaPorcentaje, BigDecimal teaMoraPorcentaje,
                              int diaCorte, int diaPago, Integer idEstadoPendiente, List<ItemDto> detalle,
                              List<CompraDto> compras, List<EstadoCuentaDto> estados) {
    }

    // ------------------------------------------------------------------ P12
    public record BitacoraDto(Integer id, LocalDateTime fechaHora, String usuario, String accion, String tabla,
                              Integer idRegistro, String valorAntes, String valorDespues, String ip) {
    }

    public record PaginaDto<T>(List<T> contenido, int pagina, int tamano, long total, int totalPaginas) {
    }
}
