package pe.fiadope.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pe.fiadope.api.dto.Dtos.*;
import pe.fiadope.api.service.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Tag(name = "P04-P10 Administrador de negocio")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class NegocioController {

    private final PanelService panel;
    private final ProductoService productos;
    private final ClienteService clientes;
    private final CompraService compras;
    private final CorteService cortes;
    private final PagoService pagos;
    private final ParametroService parametros;
    private final PdfService pdf;

    // ---------------------------------------------------------------- P04
    @Operation(summary = "HU04: panel del negocio")
    @GetMapping("/panel")
    public PanelDto panel() {
        return panel.panel();
    }

    @GetMapping("/topes")
    public TopesDto topes() {
        return parametros.topes();
    }

    // ---------------------------------------------------------------- P05
    @GetMapping("/productos")
    public List<ProductoDto> productos(@RequestParam(required = false) String categoria,
                                       @RequestParam(required = false) String q,
                                       @RequestParam(required = false) String estado) {
        return productos.listar(categoria, q, estado);
    }

    @Operation(summary = "HU05: agrega un producto al catálogo")
    @PostMapping("/productos")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductoDto crearProducto(@Valid @RequestBody ProductoRequest r) {
        return productos.crear(r);
    }

    @PutMapping("/productos/{id}")
    public ProductoDto actualizarProducto(@PathVariable Integer id, @Valid @RequestBody ProductoRequest r) {
        return productos.actualizar(id, r);
    }

    @PatchMapping("/productos/{id}/baja")
    public ProductoDto bajaProducto(@PathVariable Integer id) {
        return productos.cambiarEstado(id, false);
    }

    @PatchMapping("/productos/{id}/reactivar")
    public ProductoDto reactivarProducto(@PathVariable Integer id) {
        return productos.cambiarEstado(id, true);
    }

    @Operation(summary = "Sube la foto del producto (JPG/PNG, máx. 2 MB)")
    @PostMapping(value = "/productos/imagen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> subirImagen(@RequestParam("archivo") MultipartFile archivo) {
        return Map.of("url", productos.subirImagen(archivo));
    }

    // ---------------------------------------------------------------- P06
    @GetMapping("/clientes")
    public List<ClienteResumenDto> clientes(@RequestParam(required = false) String q) {
        return clientes.listar(q);
    }

    @GetMapping("/clientes/{id}")
    public ClienteFichaDto ficha(@PathVariable Integer id) {
        return clientes.ficha(id);
    }

    @Operation(summary = "HU06: registra un cliente con sus condiciones y su consentimiento")
    @PostMapping("/clientes")
    @ResponseStatus(HttpStatus.CREATED)
    public ClienteFichaDto crearCliente(@Valid @RequestBody ClienteRequest r) {
        return clientes.crear(r);
    }

    @PutMapping("/clientes/{id}")
    public ClienteFichaDto actualizarCliente(@PathVariable Integer id, @Valid @RequestBody ClienteRequest r) {
        return clientes.actualizar(id, r);
    }

    @PatchMapping("/clientes/{id}/baja")
    public ClienteFichaDto bajaCliente(@PathVariable Integer id) {
        return clientes.cambiarEstado(id, false);
    }

    @PatchMapping("/clientes/{id}/reactivar")
    public ClienteFichaDto reactivarCliente(@PathVariable Integer id) {
        return clientes.cambiarEstado(id, true);
    }

    @GetMapping("/clientes/{id}/compras")
    public List<CompraDto> comprasCliente(@PathVariable Integer id) {
        return compras.deCliente(id);
    }

    // ------------------------------------------------------------ P07 / P08
    @Operation(summary = "HU07: simula la compra (no guarda nada)")
    @PostMapping("/compras/simular")
    public SimulacionDto simular(@Valid @RequestBody CompraRequest r) {
        return compras.simular(r);
    }

    @Operation(summary = "HU07: registra la compra fiada")
    @PostMapping("/compras")
    @ResponseStatus(HttpStatus.CREATED)
    public CompraDto registrar(@Valid @RequestBody CompraRequest r) {
        return compras.registrar(r);
    }

    @Operation(summary = "HU08: cronograma francés de una compra en cuotas")
    @GetMapping("/compras/{id}/cronograma")
    public CronogramaDto cronograma(@PathVariable Integer id) {
        return compras.cronograma(id);
    }

    @GetMapping("/compras/{id}/cronograma/pdf")
    public ResponseEntity<byte[]> cronogramaPdf(@PathVariable Integer id) {
        return Archivos.pdf(pdf.cronograma(compras.cronograma(id)), "cronograma-" + id + ".pdf");
    }

    // ---------------------------------------------------------------- P09
    @Operation(summary = "HU09: vista previa del corte del mes")
    @GetMapping("/cortes")
    public CortePreviewDto corte(@RequestParam int anio, @RequestParam int mes) {
        return cortes.preview(anio, mes);
    }

    @Operation(summary = "HU09: genera y envía los estados de cuenta de los clientes seleccionados")
    @PostMapping("/cortes/generar")
    public CorteGeneradoDto generar(@Valid @RequestBody CorteGenerarRequest r) {
        return cortes.generar(r);
    }

    @GetMapping("/estados-cuenta/{id}/pdf")
    public ResponseEntity<byte[]> estadoPdf(@PathVariable Integer id) {
        return Archivos.pdf(cortes.pdfEstado(id), "estado-cuenta-" + id + ".pdf");
    }

    // ---------------------------------------------------------------- P10
    @Operation(summary = "HU10: desglose del pago del estado pendiente más antiguo del cliente")
    @GetMapping("/pagos/preview")
    public PagoPreviewDto previewPago(@RequestParam(required = false) Integer idCliente,
                                      @RequestParam(required = false) Integer idEstado,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return idEstado != null ? pagos.previewEstado(idEstado, fecha) : pagos.previewCliente(idCliente, fecha);
    }

    @Operation(summary = "HU10: registra el pago total (no se aceptan pagos parciales)")
    @PostMapping("/pagos")
    @ResponseStatus(HttpStatus.CREATED)
    public PagoDto pagar(@Valid @RequestBody PagoRequest r) {
        return pagos.registrar(r);
    }
}

